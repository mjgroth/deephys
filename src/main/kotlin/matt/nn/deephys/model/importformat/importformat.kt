package matt.nn.deephys.model.importformat

import com.google.common.collect.MapMaker
import kotlinx.serialization.Serializable
import matt.async.pri.MyThreadPriorities.CREATING_NEW_CACHE
import matt.async.thread.TheThreadProvider
import matt.async.thread.daemon
import matt.collect.map.dmap.withStoringDefault
import matt.collect.map.lazyMap
import matt.collect.weak.lazyWeakMap
import matt.log.profile.mem.throttle
import matt.log.warn.warn
import matt.model.flowlogic.latch.asyncloaded.DaemonLoadedValueOp
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.nn.deephys.load.test.OLD_CAT_LOAD_WARNING
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
import kotlin.collections.set

sealed interface DeephyFileObject {
    val name: String
}

private const val SUFFIX_NOT_PRESENT = "SUFFIX_NOT_PRESENT"

@Serializable
class Model(
    override val name: String,
    val suffix: String? = SUFFIX_NOT_PRESENT,
    val layers: List<Layer>,
    val classification_layer: String = "classification"
) : DeephyFileObject {
    val resolvedLayers by lazy { layers.mapIndexed { index, layer -> ResolvedLayer(layer, this@Model, index) } }
    val neurons: List<ResolvedNeuron> by lazy { resolvedLayers.flatMap { it.neurons } }
    val classificationLayer by lazy {
        resolvedLayers.first { it.isClassification(this) }
    }

    val wasLoadedWithSuffix by lazy {
        suffix != SUFFIX_NOT_PRESENT
    }

    fun infoString() = string {
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

/*../../../../../../python/deephy.py*//* https://www.rfc-editor.org/rfc/rfc8949.html */
class Test<N : Number>(
    override val name: String,
    images: List<DeephyImage<*>>,
    override val model: Model,
    override val testRAMCache: TestRAMCache,
    cats: List<Category>?,
    override val dtype: DType<N>
) : DeephyFileObject, TypedTestLike<N> {

    override fun isDoneLoading(): Boolean = true

    @Suppress("UNCHECKED_CAST")
    val images = images as List<DeephyImage<N>>


    override fun numberOfImages(): ULong = images.size.toULong()

    override fun imageAtIndex(i: Int): DeephyImage<N> = images[i]

    override val test = this


    fun putTestNeurons(map: Map<InterTestNeuron, TestNeuron<*>>) {
        @Suppress("UNCHECKED_CAST")
        testNeurons.putLoadedValue(map as Map<InterTestNeuron, TestNeuron<N>>)
    }

    val testNeurons = LoadedValueSlot<Map<InterTestNeuron, TestNeuron<N>>>()


    fun category(id: Int) = catsByID[id]!!
    /*images.find { it.category.id == id }!!.category*/

    val categories by lazy {
        if (cats != null) {
            cats.sortedBy { it.id }
        } else {
            warn(OLD_CAT_LOAD_WARNING)
            this@Test.images.map { it.category }.toSet().toList().sortedBy { it.id }
        }

    }
    val catsByID by lazy {
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

    private val activationsMatByLayerIndex = lazyWeakMap<Int, D2Array<N>> { lay ->


        //	dtype.
        val list = this@Test.images.map {
            it.weakActivations[lay]/*.asList()*/
        }/*.toNDArray()*/


        dtype.d2array(list)

        //	1

    }


    val activationsByNeuron = MapMaker()
        .weakKeys().apply {

        }
        .weakValues()
        .makeMap<InterTestNeuron, MultiArray<N, D1>>()
        .withStoringDefault {


            val theTestNeuron = testNeurons.await()[it]

            val something = try {
                theTestNeuron!!.activations.await()
            } catch (e: Exception) {
                throw Exception("Exception while getting activations of $theTestNeuron", e)
            }

            dtype.d1array(something)
            /*testNeurons!![it]!!.activations.await().asList().toNDArray()*/
            /*val myMat = activationsMatByLayerIndex[it.layer.index]
      myMat[0 ..< myMat.shape[0], it.index]*/
        }

    /*	lazyWeakMap<InterTestNeuron, MultiArray<Float, D1>> {


        //	error("todo: fix this memory leak")


        testNeurons!![it]!!.activations.await().asList().toNDArray()


        //	WeakReference(actData)





      }*/

    //  fun activationsByNeuronValueWrapped(key: InterTestNeuron): MultiArrayWrapper<N> {
    //	dtype.wr
    //  }


    val maxActivations = lazyMap<InterTestNeuron, N> { neuron ->

        /*activationsMatByLayerIndex[neuron.layer.index].slice<Float, D2, D1>(neuron.index..neuron.index, axis = 1).max()!!*/

        activationsByNeuron[neuron].max()!!


    }

    fun startPreloadingMaxActivations() {
        daemon("startPreloadingMaxActivations Thread", priority = CREATING_NEW_CACHE) {
            model.resolvedLayers.forEach {
                it.interTest.neurons.forEach {
                    maxActivations[it]
                }
            }
            println("finished preloading all maxActivations of $name!")
        }
    }


    val preds = run {
        val clsLayerIndex = model.classificationLayer.index /*attempt to remove ref to Test from thread below*/
        val ims = this@Test.images
        val nam = name
        val weakTest = WeakReference(test)
        DaemonLoadedValueOp<Map<DeephyImage<*>, Category>>(TheThreadProvider) {
            val localCatsByID = weakTest.get()!!.catsByID
            val m = HashMap<DeephyImage<*>, Category>(ims.size)
            val chunkSize = 1000
            ims.chunked(chunkSize).forEachIndexed { chunkIndex, imageChunk ->
                val lis = imageChunk.map {
                    it.weakActivations[clsLayerIndex]/*.asList()*/
                }
                val actsMat = dtype.d2array(lis)
                val argMaxResults = mk.math.argMaxD2(actsMat, 1)
                val imageStartIndex = chunkIndex * chunkSize
                argMaxResults.forEachIndexed { imageIndex, predIndex ->
                    val im = ims[imageStartIndex + imageIndex]
                    m[im] = localCatsByID[predIndex] ?: error(
                        string {
                            lineDelimited {
                                +"could not find category for predIndex=$predIndex (${localCatsByID.size} categories) of Image[index=${im.index}]"

                                +"image categories:"
                                ims.forEach {
                                    +"\t${it.category.id}"
                                }

                            }

                        }
                    )
                }
                throttle("preds of $nam")
            }

            /*val argMaxResults = mk.math.argMaxD2(activationsMatByLayerIndex[model!!.classificationLayer.index], 1)
            argMaxResults.forEachIndexed { imageIndex, predIndex ->
              m[images[imageIndex]] = category(predIndex)
            }*/

            m
        }
    }
}


