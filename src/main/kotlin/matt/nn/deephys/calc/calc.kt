package matt.nn.deephys.calc

import matt.caching.compcache.GlobalRAMComputeInput
import matt.caching.compcache.globalman.FakeCacheManager
import matt.collect.set.contents.Contents
import matt.fx.node.tex.dsl.TeXDSL
import matt.fx.node.tex.dsl.tex
import matt.lang.function.DSL
import matt.log.profile.stopwatch.tic
import matt.math.jmath.sigFigs
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.MiscActivationRatioNumerator.IMAGE_COLLECTION
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.MiscActivationRatioNumerator.MAX
import matt.nn.deephys.calc.act.Activation
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox.Companion.NORMALIZER_BUTTON_NAME
import matt.nn.deephys.gui.settings.MAX_NUM_IMAGES_IN_TOP_IMAGES
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.ImageIndex
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TestOrLoader
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import org.jetbrains.kotlinx.multik.ndarray.data.get

data class DescendingArgMaxMax<A: Number>(
  private val neuron: InterTestNeuron,
  private val test: TypedTestLike<A>,
): GlobalRAMComputeInput<List<ImageIndex>>() {
  override val cacheManager get() = test.testRAMCache

  override fun compute(): List<ImageIndex> = run {
	val theTest = test.test
	val acts = theTest.activationsByNeuron[neuron]
	val indices = test.dtype.wrap(acts).argmaxn2(
	  MAX_NUM_IMAGES_IN_TOP_IMAGES, skipInfinite = true, skipNaN = true, skipZero = true
	)
	indices.sortedByDescending {
	  acts[it].toDouble()
	}.map { ImageIndex(it) }
  }


}

data class TopImages<A: Number>(
  private val neuron: InterTestNeuron, private val test: TypedTestLike<A>, private val num: Int
): GlobalRAMComputeInput<List<ImageIndex>>() {

  override val cacheManager get() = test.testRAMCache

  override fun compute(): List<ImageIndex> = DescendingArgMaxMax(
	neuron = neuron, test = test
  )().take(num)
}


data class TopCategories<N: Number>(
  val neuron: InterTestNeuron,
  private val test: TypedTestLike<N>,
): GlobalRAMComputeInput<List<Pair<Category, RawActivation<*, *>>>>() {

  override val cacheManager get() = test.testRAMCache

  override fun compute(): List<Pair<Category, RawActivation<*, *>>> {
	val t = tic("TopCategories for ${neuron}")
	t.tic("starting to get top categories")
	val theTest = test.test
	val dtype = theTest.dtype
	val acts = theTest.activationsByNeuron[neuron]
	val activationsByCategory = mutableMapOf<Category, MutableList<N>>()
	dtype.wrap(acts).forEachIndexed { idx, act ->
	  val cat = theTest.imageAtIndex(idx).category
	  val list = activationsByCategory.getOrPut(cat) {
		mutableListOf()
	  }
	  list.add(act)
	}
	val meanActsByCat = activationsByCategory.mapValues {
	  dtype.mean(it.value)
	}
	val result = meanActsByCat.entries.sortedByDescending { it.value.toDouble() }.take(5).map {
	  it.key to test.dtype.rawActivation(it.value)
	}
	t.toc("got result")
	return result
  }
}


private const val NUM_TOP_NEURONS = 25

interface TopNeuronsCalcType {
  operator fun invoke(): List<NeuronWithActivation<*>>
}

data class NeuronWithActivation<A: Number>(val neuron: InterTestNeuron, val activation: Activation<A, *>)

data class TopNeurons<N: Number>(
  val images: Contents<DeephyImage<N>>,
  val layer: InterTestLayer,
  private val test: TypedTestLike<N>,
  private val denomTest: TypedTestLike<N>?,/*val normalized: Boolean,*/
  val forcedNeuronIndices: List<Int>? = null
): GlobalRAMComputeInput<List<NeuronWithActivation<N>>>(), TopNeuronsCalcType {

  /*small possibility of memory leaks when images is empty, but this is still way better than before*/
  override val cacheManager get() = images.firstOrNull()?.testLoader?.testRAMCache ?: FakeCacheManager

  override fun compute(): List<NeuronWithActivation<N>> {
	if (images.isEmpty() && forcedNeuronIndices == null) return listOf()

	val neurons = forcedNeuronIndices?.let {
	  it.map { layer.neurons[it] }
	} ?: layer.neurons

	val dType = test.dtype
	if (denomTest != null) {
	  require(dType == denomTest.dtype)
	}


	val neuronsWithActs = neurons.map { neuron ->
	  val act = if (denomTest != null) ActivationRatioCalc(
		numTest = test, denomTest = denomTest, neuron = neuron, images = images
	  )()
	  else if (images.isEmpty()) neuron.maxActivationIn(test)
	  else neuron.averageActivation(images, dType = dType)
	  NeuronWithActivation(neuron, act)
	}



	return if (forcedNeuronIndices == null) {
	  val r = neuronsWithActs.filterNot {
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


data class ActivationRatioCalc<A: Number>(
  val numTest: TypedTestLike<A>,
  private val images: Contents<DeephyImage<A>>,
  val denomTest: TypedTestLike<A>,
  private val neuron: InterTestNeuron
): GlobalRAMComputeInput<Activation<A, *>>() {

  override val cacheManager get() = FakeCacheManager /*this ComputeCache was taking up a TON of memory.*//*override val cacheManager get() = numTest.testRAMCache*/ /*could be either one.*/ /*small possibility for memory leaks if the user gets keeps swapping out denomTest without swapping out numTest, but this is still a WAY better mechanism than before*/


  companion object {
	sealed interface ActivationRatioNumerator
	class SINGLE_IMAGE(val id: Int): ActivationRatioNumerator
	enum class MiscActivationRatioNumerator: ActivationRatioNumerator {
	  IMAGE_COLLECTION, MAX
	}


	fun latexTechnique(num: ActivationRatioNumerator): TeXDSL {
	  val denom: DSL<TeXDSL> = { text("max activation of this neuron in $NORMALIZER_BUTTON_NAME") }
	  return when (num) {
			   MAX              -> tex {
				 frac(
				   num = { text("max activation of this neuron in this test") },
				   denom = denom
				 )
			   }

			   is SINGLE_IMAGE  -> tex {
				 frac(
				   num = { text("raw activation of this neuron for image ${num.id}") },
				   denom = denom
				 )
			   }

			   IMAGE_COLLECTION -> tex {
				 frac(
				   num = { text("average activation of this neuron for selected images") },
				   denom = denom
				 )
			   }
			 }*100
	}
  }

  /*TODO: make this a lazy val so I don't need to make params above vals*/
  override fun compute(): Activation<A, *> {

	val dType = numTest.dtype
	val r = if (images.isEmpty()) {
	  if (numTest == denomTest) dType.alwaysOneActivation()
	  else {
		val num = numTest.test.maxActivations[neuron]
		val denom = denomTest.test.maxActivations[neuron]

		val n = dType.div(num, denom)
		dType.activationRatio(n)
	  }
	} else {
	  val num = neuron.averageActivation(images, dType = dType).value
	  val denom = denomTest.test.maxActivations[neuron]
	  dType.activationRatio(dType.div(num, denom))
	}
	return r
  }

}

data class ImageSoftMaxDenom<N: Number>(
  private val image: DeephyImage<N>, private val testLoader: TestOrLoader
): GlobalRAMComputeInput<N>() {

  override val cacheManager get() = testLoader.testRAMCache

  override fun compute(): N {
	val clsLay = testLoader.model.classificationLayer
	val preds = image.activationsFor(clsLay.interTest)

	val dtype = image.dtype
	require(dtype == testLoader.dtype)

	val li = preds.map { dtype.exp(it) }

	val su = dtype.sum(li)

	return su

  }
}

data class ImageTopPredictions<N: Number>(
  private val image: DeephyImage<N>, private val testLoader: TypedTestLike<N>
): GlobalRAMComputeInput<List<Pair<Category, N>>>() {

  override val cacheManager get() = testLoader.testRAMCache

  override fun compute(): List<Pair<Category, N>> {
	val dtype = testLoader.dtype
	val clsLay = testLoader.model.classificationLayer
	val preds = image.activationsFor(clsLay.interTest)
	val softMaxDenom = ImageSoftMaxDenom(image, testLoader)()
	preds.withIndex().sortedBy {
	  it.value.toDouble()
	}
	return preds.withIndex().sortedBy { it.value.toDouble() }.reversed().take(5).map { thePred ->
	  val exactPred = (dtype.div(dtype.exp(thePred.value), softMaxDenom))
	  val theCategory = testLoader.test.category(thePred.index)
	  theCategory to exactPred
	}
  }
}

data class CategoryAccuracy(
  private val category: Category, private val testLoader: TypedTestLike<*>
): GlobalRAMComputeInput<Double?>() {

  override val cacheManager get() = testLoader.testRAMCache

  override fun compute(): Double? {

	val images = testLoader.test.imagesWithGroundTruth(category)
	if (images.isEmpty()) {
	  return null
	}

	val r = images.map {
	  if (testLoader.test.preds.await()[it] == category) 1.0 else 0.0
	}.let { it.sum()/it.size }

	return r
  }


  fun formatted() =
	compute().let { if (it == null) "Cannot calculate accuracy because no images have groundtruth \"$category\"" else "${(it*100).sigFigs(3)}%" }

}

data class CategoryFalsePositivesSorted<N: Number>(
  private val category: Category, private val testLoader: TypedTestLike<N>
): GlobalRAMComputeInput<List<DeephyImage<N>>>() {

  override val cacheManager get() = testLoader.testRAMCache

  companion object {
	const val blurb = "false positives sorted so that the images with the highest prediction value (after softmax) are first"
  }

  override fun compute(): List<DeephyImage<N>> = run {
	testLoader.test.imagesWithoutGroundTruth(category).map {
	  it to testLoader.test.preds.await()[it]
	}.filter {
	  it.second == category
	}.map {
	  it.first to ImageTopPredictions(it.first, testLoader)().first()
	}.sortedBy {
	  it.second.second.toDouble()
	}.reversed().map {
	  it.first
	}
  }
}

data class CategoryFalseNegativesSorted<N: Number>(
  private val category: Category, private val testLoader: TypedTestLike<N>
): GlobalRAMComputeInput<List<DeephyImage<N>>>() {

  override val cacheManager get() = testLoader.testRAMCache

  companion object {
	const val blurb = "false negatives sorted so that the images with the highest prediction value (after softmax) are first"
  }

  override fun compute() = run {
	testLoader.test.imagesWithGroundTruth(category).map {
	  it to testLoader.test.preds.await()[it]
	}.filter {
	  it.second != category
	}.map {
	  it.first to ImageTopPredictions(it.first, testLoader)().first()
	}.sortedBy {
	  it.second.second.toDouble()
	}.reversed().map {
	  it.first
	}
  }
}


