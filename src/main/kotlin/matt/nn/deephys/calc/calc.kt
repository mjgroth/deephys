package matt.nn.deephys.calc

import matt.caching.compcache.ComputeInput
import matt.caching.compcache.FakeCacheComputeInput
import matt.caching.compcache.findOrCompute
import matt.caching.compcache.globalman.FakeCacheManager
import matt.caching.compcache.invoke
import matt.codegen.tex.TeXDSL
import matt.codegen.tex.tex
import matt.collect.set.contents.Contents
import matt.lang.assertions.require.requireEquals
import matt.lang.function.Dsl
import matt.math.numalg.precision.withPrecision
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.MiscActivationRatioNumerator.IMAGE_COLLECTION
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.MiscActivationRatioNumerator.MAX
import matt.nn.deephys.calc.act.Activation
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox.Companion.NORMALIZER_BUTTON_NAME
import matt.nn.deephys.gui.fix.TestAndSomeImages
import matt.nn.deephys.gui.settings.MAX_NUM_IMAGES_IN_TOP_IMAGES
import matt.nn.deephys.load.test.testcache.TestRAMCache
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.ImageIndex
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TestOrLoader
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import org.jetbrains.kotlinx.multik.ndarray.data.get

abstract class TestComputeInput<O> : ComputeInput<O, TestRAMCache>()

data class DescendingArgMaxMax<A : Number>(
    private val neuron: InterTestNeuron,
    private val test: TypedTestLike<A>
) : TestComputeInput<List<ImageIndex>>() {

    context(TestRAMCache)
    override fun compute(): List<ImageIndex> =
        run {
            val theTest = test.test
            val acts = theTest.activationsByNeuron[neuron]
            val indices =
                test.dtype.wrap(acts).argmaxn2(
                    MAX_NUM_IMAGES_IN_TOP_IMAGES, skipInfinite = true, skipNaN = true, skipZero = true
                )
            indices.sortedByDescending {
                acts[it].toDouble()
            }.map { ImageIndex(it) }
        }
}

data class TopImages<A : Number>(
    private val neuron: InterTestNeuron,
    private val test: TypedTestLike<A>,
    private val num: Int
) : TestComputeInput<List<ImageIndex>>() {


    context(TestRAMCache)
    override fun compute(): List<ImageIndex> =
        DescendingArgMaxMax(
            neuron = neuron, test = test
        )().take(num)
}


data class TopCategories<N : Number>(
    val neuron: InterTestNeuron,
    private val test: TypedTestLike<N>
) : TestComputeInput<List<Pair<Category, RawActivation<*, *>>>>() {


    context(TestRAMCache)
    override fun compute(): List<Pair<Category, RawActivation<*, *>>> {
        val theTest = test.test
        val dtype = theTest.dtype
        val acts = theTest.activationsByNeuron[neuron]
        val activationsByCategory = mutableMapOf<Category, MutableList<N>>()
        dtype.wrap(acts).forEachIndexed { idx, act ->
            val cat = theTest.imageAtIndex(idx).category
            val list =
                activationsByCategory.getOrPut(cat) {
                    mutableListOf()
                }
            list.add(act)
        }
        val meanActsByCat =
            activationsByCategory.mapValues {
                dtype.mean(it.value)
            }
        val result =
            meanActsByCat.entries.sortedByDescending { it.value.toDouble() }.take(5).map {
                it.key to test.dtype.rawActivation(it.value)
            }
        return result
    }
}


private const val NUM_TOP_NEURONS = 25

/*
interface TopNeuronsCalcType {
    context(TestRAMCache)
    operator fun invoke(): List<NeuronWithActivation<*>>
}
*/

data class NeuronWithActivation<A : Number>(
    val neuron: InterTestNeuron,
    val activation: Activation<A, *>
)

data class TopNeurons<N : Number>(
    val testAndImages: TestAndSomeImages<N>,
    val layer: InterTestLayer,
    private val denomTest: TypedTestLike<*>?, /*val normalized: Boolean,*/
    val forcedNeuronIndices: List<Int>? = null
) : TestComputeInput<List<NeuronWithActivation<N>>>()/*, TopNeuronsCalcType*/ {

    /*small possibility of memory leaks when images is empty, but this is still way better than before*/

    context (TestRAMCache)
    override fun compute(): List<NeuronWithActivation<N>> {
        val images = testAndImages.images
        val test = testAndImages.test
        if (images.isEmpty() && forcedNeuronIndices == null) return listOf()

        val neurons =
            forcedNeuronIndices?.let {
                it.map { layer.neurons[it] }
            } ?: layer.neurons

        val dType = test.dtype
        if (denomTest != null) {
            requireEquals(dType, denomTest.dtype)
        }


        val neuronsWithActs =
            neurons.map { neuron ->
                val act =
                    if (denomTest != null) with(FakeCacheManager) {
                        ActivationRatioCalc(
                            numTest = test,
                            denomTest = denomTest,
                            neuron = neuron,
                            images = images
                        )()
                    }
                    else if (images.isEmpty()) neuron.maxActivationIn(test)
                    else neuron.averageActivation(images, dType = dType)
                NeuronWithActivation(neuron, act)
            }



        return if (forcedNeuronIndices == null) {
            val r =
                neuronsWithActs.filterNot {
                    it.activation.isNaN || it.activation.isInfinite || it.activation.isZero
                }.sortedByDescending {
                    it.activation.value.toDouble()
                }.take(NUM_TOP_NEURONS)

            r
        } else {
            val r = neuronsWithActs
            r
        }
    }
}


data class ActivationRatioCalc<A : Number>(
    val numTest: TypedTestLike<A>,
    private val images: Contents<DeephyImage<A>>,
    val denomTest: TypedTestLike<*>,
    private val neuron: InterTestNeuron
) : FakeCacheComputeInput<Activation<A, *>>()  /*because this is taking up way too much memory*/ {

    /*this ComputeCache was taking up a TON of memory.






    override val cacheManager get() = numTest.testRAMCache


    could be either one.


    small possibility for memory leaks if the user gets keeps swapping out denomTest without swapping out numTest, but this is still a WAY better mechanism than before*/


    companion object {
        sealed interface ActivationRatioNumerator
        class SingleImage(val id: Int) : ActivationRatioNumerator
        enum class MiscActivationRatioNumerator : ActivationRatioNumerator {
            IMAGE_COLLECTION, MAX
        }


        fun latexTechnique(num: ActivationRatioNumerator): TeXDSL {
            val denom: Dsl<TeXDSL> = { text("max activation of this neuron in $NORMALIZER_BUTTON_NAME") }
            return when (num) {
                MAX              ->
                    tex {
                        frac(
                            num = { text("max activation of this neuron in this test") },
                            denom = denom
                        )
                    }

                is SingleImage ->
                    tex {
                        frac(
                            num = { text("raw activation of this neuron for image ${num.id}") },
                            denom = denom
                        )
                    }

                IMAGE_COLLECTION ->
                    tex {
                        frac(
                            num = { text("average activation of this neuron for selected images") },
                            denom = denom
                        )
                    }
            } * 100
        }
    }

    context(FakeCacheManager)
    override fun compute(): Activation<A, *> {
        println("make this a lazy val so I don't need to make params above vals")
        val dType = numTest.dtype
        val r =
            if (images.isEmpty()) {
                if (numTest == denomTest) dType.alwaysOneActivation()
                else {
                    val num = numTest.test.maxActivations[neuron]
                    val denom = denomTest.test.maxActivations[neuron]

                    val n = dType.div(num, dType.cast(denom))
                    dType.activationRatio(n)
                }
            } else {
                val num = neuron.averageActivation(images, dType = dType).value
                val denom = denomTest.test.maxActivations[neuron]
                dType.activationRatio(dType.div(num, dType.cast(denom)))
            }
        return r
    }
}

data class ImageSoftMaxDenom<N : Number>(
    private val image: DeephyImage<N>,
    private val testLoader: TestOrLoader
) : TestComputeInput<N>() {


    context(TestRAMCache)
    override fun compute(): N {
        val clsLay = testLoader.model.classificationLayer
        val preds = image.activationsFor(clsLay.interTest)

        val dtype = image.dtype
        requireEquals(dtype, testLoader.dtype)

        val li = preds.map { dtype.exp(it) }

        val su = dtype.sum(li)

        return su
    }
}

data class ImageTopPredictions<N : Number>(
    private val image: DeephyImage<N>
) : TestComputeInput<List<Pair<Category, N>>>() {


    context(TestRAMCache)
    override fun compute(): List<Pair<Category, N>> {
        val dtype = image.testLoader.dtype
        val clsLay = image.testLoader.model.classificationLayer
        val preds = image.activationsFor(clsLay.interTest)
        val calculation = ImageSoftMaxDenom(image, image.testLoader)
        /*calculation.compute()
        val n: N = calculation.compute()*/

        val softMaxDenom = calculation.findOrCompute(dtype)
        preds.withIndex().sortedBy {
            it.value.toDouble()
        }
        return preds.withIndex().sortedBy { it.value.toDouble() }.reversed().take(5).map { thePred ->
            val exactPred = (dtype.div(dtype.exp(thePred.value), softMaxDenom))
            val theCategory = image.testLoader.test.category(thePred.index)
            theCategory to exactPred
        }
    }
}

data class CategoryAccuracy(
    private val category: Category,
    private val testLoader: TypedTestLike<*>
) : TestComputeInput<Double?>() {


    context(TestRAMCache)
    override fun compute(): Double? {

        val images = testLoader.test.imagesWithGroundTruth(category)
        if (images.isEmpty()) {
            return null
        }

        val r =
            images.map {
                if (testLoader.test.preds.await()[it] == category) 1.0 else 0.0
            }.let { it.sum() / it.size }

        return r
    }

    context(TestRAMCache)
    fun formatted() =
        compute().let {
            if (it == null) "Cannot calculate accuracy because no images have groundtruth \"$category\"" else "${
                (it * 100).withPrecision(
                    3
                )
            }%"
        }
}

data class CategoryFalsePositivesSorted<N : Number>(
    private val category: Category,
    private val testLoader: TypedTestLike<N>
) : TestComputeInput<List<DeephyImage<N>>>() {


    companion object {
        const val blurb =
            "false positives sorted so that the images with the highest prediction value (after softmax) are first"
    }

    context(TestRAMCache)
    override fun compute(): List<DeephyImage<N>> {
        val r =
            testLoader.test.imagesWithoutGroundTruth(category).map {
                it to testLoader.test.preds.await()[it]
            }.filter {
                it.second == category
            }.map {
                it.first to ImageTopPredictions(it.first)().first()
            }.sortedBy {
                it.second.second.toDouble()
            }.reversed().map {
                it.first
            }


        return r
    }
}

data class CategoryFalseNegativesSorted<N : Number>(
    private val category: Category,
    private val testLoader: TypedTestLike<N>
) : TestComputeInput<List<DeephyImage<N>>>() {


    companion object {
        const val blurb =
            "false negatives sorted so that the images with the highest prediction value (after softmax) are first"
    }

    context(TestRAMCache)
    override fun compute() =
        run {
            testLoader.test.imagesWithGroundTruth(category).map {
                it to testLoader.test.preds.await()[it]
            }.filter {
                it.second != category
            }.map {
                it.first to ImageTopPredictions(it.first)().first()
            }.sortedBy {
                it.second.second.toDouble()
            }.reversed().map {
                it.first
            }
        }
}


