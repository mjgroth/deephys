package matt.nn.deephys.model.importformat

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import matt.async.pri.MyThreadPriority.CREATING_NEW_CACHE
import matt.async.thread.TheThreadProvider
import matt.async.thread.daemon
import matt.collect.map.dmap.LazyKeyed
import matt.collect.map.dmap.lazyKeyed
import matt.collect.weak.lazy.weakSynchronizedLazyKeyed
import matt.log.profile.mem.throttle
import matt.model.flowlogic.latch.asyncloaded.DaemonLoadedValueOp
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.model.k.log.warnPrefixedCompat
import matt.nn.deephys.load.test.OLD_CAT_LOAD_WARNING
import matt.nn.deephys.load.test.PostDtypeTestLoader
import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.testcache.TestRAMCache
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.ResolvedNeuron
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.layer.Layer
import matt.nn.deephys.model.importformat.neuron.TestNeuron
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.prim.str.mybuild.api.string
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.MultiArray
import org.jetbrains.kotlinx.multik.ndarray.operations.forEachIndexed
import org.jetbrains.kotlinx.multik.ndarray.operations.max
import java.lang.ref.WeakReference

sealed interface DeephyFileObject {
    val name: String
}

private const val SUFFIX_NOT_PRESENT = "SUFFIX_NOT_PRESENT"

@Serializable
class Model(
    override val name: String,
    private val suffix: String? = SUFFIX_NOT_PRESENT,
    val layers: List<Layer>,
    @Suppress("ConstructorParameterNaming") val classification_layer: String = "classification"
) : DeephyFileObject {
    val resolvedLayers by lazy {
        layers.mapIndexed { index, layer -> ResolvedLayer(layer, this@Model, index) }
    }
    val neurons: List<ResolvedNeuron> by lazy { resolvedLayers.flatMap { it.neurons } }
    val classificationLayer by lazy {
        resolvedLayers.first { it.isClassification(this) }
    }

    val wasLoadedWithSuffix by lazy {
        suffix != SUFFIX_NOT_PRESENT
    }

    fun infoString() =
        string {
            lineDelimited {
                +"Model:"
                +"\tname=$name"
                +"\tlayers:"
                layers.forEach {
                    +"\t\t${it.layerID} (${it.neurons.size} neurons)"
                }
            }
        }
}

/*

../../../../../../python/deephy.py


 https://www.rfc-editor.org/rfc/rfc8949.html

 */
class Test<N : Number>(
    override val name: String,
    val images: List<DeephyImage<N>>,
    override val model: Model,
    override val testRAMCache: TestRAMCache,
    cats: List<Category>?,
    override val dtype: DType<N>,
    override val post: PostDtypeTestLoader<N>
) : DeephyFileObject, TypedTestLike<N> {

    override fun isDoneLoading(): Boolean = true

    override fun numberOfImages(): ULong = images.size.toULong()

    override fun imageAtIndex(i: Int): DeephyImage<N> = images[i]

    override val test = this

    fun putTestNeurons(map: Map<InterTestNeuron, TestNeuron<N>>) {

        testNeurons.putLoadedValue(map)
    }

    private val testNeurons = LoadedValueSlot<Map<InterTestNeuron, TestNeuron<N>>>()

    fun category(id: Int) = catsByID[id]!!
    /*images.find { it.category.id == id }!!.category*/

    val categories by lazy {
        if (cats != null) {
            cats.sortedBy { it.id }
        } else {
            warnPrefixedCompat(OLD_CAT_LOAD_WARNING)
            this@Test.images.map { it.category }.toSet().toList().sortedBy { it.id }
        }
    }
    private val catsByID by lazy {
        categories.associateBy { it.id }
    }

    private val imagesByCategoryID by lazy {
        val r = categories.associateWith { setOf<DeephyImage<N>>() }.toMutableMap()
        val toPut = this@Test.images.groupBy { it.category }.mapValues { it.value.toSet() }
        r.putAll(toPut)
        r.mapKeys { it.key.id }
    }

    fun imagesWithGroundTruth(category: Category): Set<DeephyImage<N>> = imagesByCategoryID[category.id] ?: setOf()
    fun imagesWithoutGroundTruth(category: Category) = images - (imagesByCategoryID[category.id] ?: setOf())

    init {
        listOf(listOf(1.0)).toNDArray()
    }

    @Suppress("unused")
    private val activationsMatByLayerIndex =
        lazyKeyed<Int, D2Array<N>>(
            LazyThreadSafetyMode.SYNCHRONIZED /*idk, safest guess*/
        ) { lay ->
            val list =
                this@Test.images.map {
                    it.weakActivations[lay]
                }
            dtype.d2array(list)
        }

    val activationsByNeuron: LazyKeyed<InterTestNeuron, MultiArray<N, D1>> =
        weakSynchronizedLazyKeyed<InterTestNeuron, MultiArray<N, D1>> {
            val theTestNeuron = testNeurons.await()[it]
            val something =
                try {
                    theTestNeuron!!.activations.await()
                } catch (e: Exception) {
                    throw Exception("Exception while getting activations of $theTestNeuron", e)
                }

            dtype.d1array(something)
        }
     /*   MapMaker()
            .weakKeys().apply {
            }
            .weakValues()
            .makeMap<InterTestNeuron, MultiArray<N, D1>>()
            .withStoringDefault {

            }*/

    val maxActivations =
        lazyKeyed<InterTestNeuron, N>(LazyThreadSafetyMode.SYNCHRONIZED /*unsure, safest guess*/) { neuron ->
            activationsByNeuron[neuron].max()!!
        }

    fun startPreloadingMaxActivations() {
        val _ =
            daemon("startPreloadingMaxActivations Thread", priority = CREATING_NEW_CACHE) {
                model.resolvedLayers.forEach { resolvedLayer ->
                    resolvedLayer.interTest.neurons.forEach {
                        val _ = maxActivations[it]
                    }
                }
                println("finished preloading all maxActivations of $name!")
            }
    }

    val preds =
        run {
            /*attempt to remove ref to Test from thread below*/
            val clsLayerIndex = model.classificationLayer.index
            val ims = this@Test.images
            val nam = name
            val weakTest = WeakReference(test)
            DaemonLoadedValueOp<Map<DeephyImage<*>, Category>>(TheThreadProvider) {
                val localCatsByID = weakTest.get()!!.catsByID
                val m = HashMap<DeephyImage<*>, Category>(ims.size)
                val chunkSize = 1000
                ims.chunked(chunkSize).forEachIndexed { chunkIndex, imageChunk ->
                    val lis =
                        imageChunk.map {
                            it.weakActivations[clsLayerIndex]
                        }
                    val actsMat = dtype.d2array(lis)
                    val argMaxResults = mk.math.argMaxD2(actsMat, 1)
                    val imageStartIndex = chunkIndex * chunkSize
                    argMaxResults.forEachIndexed { imageIndex, predictionIndex ->
                        val im = ims[imageStartIndex + imageIndex]
                        m[im] = localCatsByID[predictionIndex] ?: error(
                            string {
                                lineDelimited {
                                    +"could not find category for predictionIndex=$predictionIndex (${localCatsByID.size} categories) of Image[index=${im.index}]"

                                    +"image categories:"
                                    ims.forEach {
                                        +"\t${it.category.id}"
                                    }
                                }
                            }
                        )
                    }
                    runBlocking {
                        throttle("preds of $nam")
                    }
                }
                m
            }
        }
}
