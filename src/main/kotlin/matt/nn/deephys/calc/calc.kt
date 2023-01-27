package matt.nn.deephys.calc

import matt.caching.compcache.globalman.FakeCacheManager
import matt.caching.compcache.globalman.GlobalRAMComputeCacheManager
import matt.caching.compcache.timed.TimedComputeInput
import matt.collect.set.contents.Contents
import matt.log.warn.warnOnce
import matt.nn.deephys.calc.act.Activation
import matt.nn.deephys.calc.act.NormalActivation
import matt.nn.deephys.calc.act.NormalActivation.Companion.NORMALIZED_ACT_SYMBOL
import matt.nn.deephys.calc.act.RawActivation.Companion.RAW_ACT_SYMBOL
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.ImageIndex
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TestOrLoader
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.nn.deephys.state.DeephySettings

data class NormalizedAverageActivation<N: Number>(
  private val neuron: InterTestNeuron,
  private val images: Contents<DeephyImage<N>>,
  private val test: TypedTestLike<N>,
): DeephysComputeInput<NormalActivation<N, *>>() {

  /*small possibility of memory leaks when images is empty, but this is still way better than before*/
  override val cacheManager get() = images.firstOrNull()?.testLoader?.testRAMCache ?: GlobalRAMComputeCacheManager

  companion object {
	const val normalizeTopNeuronsBlurb =
	  "Normalized Activation ($NORMALIZED_ACT_SYMBOL) = Raw Activation ($RAW_ACT_SYMBOL) / max(activation for each image for this neuron)"
  }

  override fun timedCompute(): NormalActivation<N, *> {
	val num = neuron.averageActivation(images, dType = test.dtype).value

	val denom = test.test.maxActivations[neuron]

	val r = test.dtype.div(num, denom)
	return test.dtype.normalActivation(r)
  }

  override fun equals(other: Any?): Boolean {
	return other is NormalizedAverageActivation<*> && other.neuron == this.neuron && other.images == images
  }

  override fun hashCode(): Int {
	var result = neuron.hashCode()
	result = 31*result + images.hashCode()
	return result
  }


}


data class TopImages<A: Number>(
  private val neuron: InterTestNeuron,
  private val test: TypedTestLike<A>,
  private val num: Int
): DeephysComputeInput<List<ImageIndex>>() {

  override val cacheManager get() = test.testRAMCache

  override fun timedCompute(): List<ImageIndex> = run {
	val theTest = test.test

	//	theTest.argmaxn2OfNeuron(neuron,num)

	val acts = theTest.activationsByNeuron[neuron]

	//	acts
	val indices = test.dtype.wrap(acts).argmaxn2(num, skipInfinite = true, skipNaN = true)

	/*val indices = acts.argmaxn2(num)*/
	indices.map { ImageIndex(it) }
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
  private val denomTest: TypedTestLike<N>?,
  val normalized: Boolean,
  val forcedNeuronIndices: List<Int>? = null
): DeephysComputeInput<List<NeuronWithActivation<N>>>(), TopNeuronsCalcType {

  /*small possibility of memory leaks when images is empty, but this is still way better than before*/
  override val cacheManager get() = images.firstOrNull()?.testLoader?.testRAMCache ?: FakeCacheManager

  override fun timedCompute(): List<NeuronWithActivation<N>> {
	if (images.isEmpty() && denomTest == null) return listOf()

	val neurons = forcedNeuronIndices?.let {
	  it.map { layer.neurons[it] }
	} ?: layer.neurons

	val dType = test.dtype
	if (denomTest != null) {
	  require(dType == denomTest.dtype)
	}


	val neuronsWithActs = neurons.map { neuron ->
	  val act = if (denomTest != null) ActivationRatioCalc(
		numTest = test,
		denomTest = denomTest,
		neuron = neuron,
		images = images
	  )()
	  else if (normalized) NormalizedAverageActivation(neuron, images, test)()
	  else neuron.averageActivation(images, dType = dType)
	  NeuronWithActivation(neuron, act)
	}

	/*


		neuronsWithActs.filterNot {
		  it.activation.isNaN
		}.sortedBy {
		  it.activation
		}
	*/
	//
	//	val temp = neuronsWithActs.filterNot {
	//	  it.activation.isNaN
	//	}
	//	  .sortedBy {
	//		it.activation
	//	  }
	//	  .reversed()
	//	  .take(NUM_TOP_NEURONS)

	return if (forcedNeuronIndices == null) {
	  val r = neuronsWithActs.filterNot {
		it.activation.isNaN || it.activation.isInfinite
	  }
		.sortedBy {
		  it.activation.value.toDouble()
		}
		.reversed()
		.take(NUM_TOP_NEURONS)

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
): DeephysComputeInput<Activation<A, *>>() {

  override val cacheManager get() = numTest.testRAMCache /*could be either one.*/ /*small possibility for memory leaks if the user gets keeps swapping out denomTest without swapping out numTest, but this is still a WAY better mechanism than before*/

  companion object {
	const val technique =
	  "This value is the ratio between the maximum activation of this neuron and the maximum activation of the InD neuron"
  }

  /*TODO: make this a lazy val so I don't need to make params above vals*/
  override fun timedCompute(): Activation<A, *> {
	/*val dType = DType.leastPrecise(numTest.dtype, denomTest.dtype)*/
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
	if (r.isNaN || r.isInfinite) {
	  println(
		"""
		f=${r.value}
		images.size=${images.size}
		numTest.test.maxActivations[neuron]=${numTest.test.maxActivations[neuron]}
		denomTest.test.maxActivations[neuron]=${denomTest.test.maxActivations[neuron]}
		${
		  if (images.isNotEmpty()) "neuron.averageActivation(images).value=${
			neuron.averageActivation(
			  images,
			  dType = dType
			).value
		  }, denomTest.test.maxActivations[neuron]=${denomTest.test.maxActivations[neuron]}" else ""
		}
	  """.trimIndent()
	  )
	}
	return r
  }

}

data class ImageSoftMaxDenom<N: Number>(
  private val image: DeephyImage<N>,
  private val testLoader: TestOrLoader
): DeephysComputeInput<N>() {

  override val cacheManager get() = testLoader.testRAMCache

  override fun timedCompute(): N {
	val clsLay = testLoader.model.classificationLayer
	val preds = image.activationsFor(clsLay.interTest)

	val dtype = image.dtype
	require(dtype == testLoader.dtype)

	val li = preds.map { dtype.exp(it) }

	val su = dtype.sum(li)

	return su

	//	return preds.sumOf {
	//
	//	}
  }
}

data class ImageTopPredictions<N: Number>(
  private val image: DeephyImage<N>,
  private val testLoader: TypedTestLike<N>
): DeephysComputeInput<List<Pair<Category, N>>>() {

  override val cacheManager get() = testLoader.testRAMCache

  override fun timedCompute(): List<Pair<Category, N>> {
	val dtype = testLoader.dtype
	val clsLay = testLoader.model.classificationLayer
	val preds = image.activationsFor(clsLay.interTest)
	val softMaxDenom = ImageSoftMaxDenom(image, testLoader)()
	warnOnce("bad toDouble x2")
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
  private val category: Category,
  private val testLoader: TypedTestLike<*>
): DeephysComputeInput<Double>() {

  override val cacheManager get() = testLoader.testRAMCache

  override fun timedCompute(): Double {
	val r = testLoader.test.imagesWithGroundTruth(category).map {
	  if (testLoader.test.preds.await()[it] == category) 1.0 else 0.0
	}.let { it.sum()/it.size }

	return r
  }
}

data class CategoryFalsePositivesSorted<N: Number>(
  private val category: Category,
  private val testLoader: TypedTestLike<N>
): DeephysComputeInput<List<DeephyImage<N>>>() {

  override val cacheManager get() = testLoader.testRAMCache

  companion object {
	const val blurb =
	  "false positives sorted so that the images with the highest prediction value (after softmax) are first"
  }

  override fun timedCompute(): List<DeephyImage<N>> = run {
	warnOnce("bad toDouble here")
	testLoader.test.imagesWithoutGroundTruth(category)
	  .map {
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
  private val category: Category,
  private val testLoader: TypedTestLike<N>
): DeephysComputeInput<List<DeephyImage<N>>>() {

  override val cacheManager get() = testLoader.testRAMCache

  companion object {
	const val blurb =
	  "false negatives sorted so that the images with the highest prediction value (after softmax) are first"
  }

  override fun timedCompute() = run {
	warnOnce("bad toDouble here")
	testLoader.test.imagesWithGroundTruth(category)
	  .map {
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


abstract class DeephysComputeInput<O>(): TimedComputeInput<O>() {
  override val stopwatchEnabled get() = DeephySettings.verboseLogging.value
}