package matt.nn.deephys.calc

import matt.caching.compcache.globalman.FakeCacheManager
import matt.caching.compcache.globalman.GlobalRAMComputeCacheManager
import matt.caching.compcache.timed.TimedComputeInput
import matt.collect.set.contents.Contents
import matt.math.mat.argmaxn.argmaxn2
import matt.math.reduce.sumOf
import matt.nn.deephys.calc.act.Activation
import matt.nn.deephys.calc.act.ActivationRatio
import matt.nn.deephys.calc.act.AlwaysOneActivation
import matt.nn.deephys.calc.act.NormalActivation
import matt.nn.deephys.calc.act.NormalActivation.Companion.NORMALIZED_ACT_SYMBOL
import matt.nn.deephys.calc.act.RawActivation.Companion.RAW_ACT_SYMBOL
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.ImageIndex
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TestOrLoader
import matt.nn.deephys.state.DeephySettings
import kotlin.math.exp

data class NormalizedAverageActivation(
  private val neuron: InterTestNeuron,
  private val images: Contents<DeephyImage>,
  private val test: TestOrLoader
): DeephysComputeInput<NormalActivation>() {

  /*small possibility of memory leaks when images is empty, but this is still way better than before*/
  override val cacheManager get() = images.firstOrNull()?.testLoader?.testRAMCache ?: GlobalRAMComputeCacheManager

  companion object {
	const val normalizeTopNeuronsBlurb =
	  "Normalized Activation ($NORMALIZED_ACT_SYMBOL) = Raw Activation ($RAW_ACT_SYMBOL) / max(activation for each image for this neuron)"
  }

  override fun timedCompute(): NormalActivation {
	return NormalActivation(neuron.averageActivation(images).value/test.test.maxActivations[neuron])
  }

  override fun equals(other: Any?): Boolean {
	return other is NormalizedAverageActivation && other.neuron == this.neuron && other.images == images
  }

  override fun hashCode(): Int {
	var result = neuron.hashCode()
	result = 31*result + images.hashCode()
	return result
  }


}


data class TopImages(
  private val neuron: InterTestNeuron,
  private val test: TestLoader,
  private val num: Int
): DeephysComputeInput<List<ImageIndex>>() {

  override val cacheManager get() = test.testRAMCache

  override fun timedCompute(): List<ImageIndex> = run {
	val theTest = test.awaitFinishedTest()
	val acts = theTest.activationsByNeuron[neuron]
	val indices = acts.argmaxn2(num)
	indices.map { ImageIndex(it) }
  }
}

private const val NUM_TOP_NEURONS = 25

interface TopNeuronsCalcType {
  operator fun invoke(): List<NeuronWithActivation>
}

data class NeuronWithActivation(val neuron: InterTestNeuron, val activation: Activation<*>)

data class TopNeurons(
  private val images: Contents<DeephyImage>,
  private val layer: InterTestLayer,
  private val test: TestOrLoader,
  private val denomTest: TestOrLoader?,
  val normalized: Boolean,
  val forcedNeuronIndices: List<Int>? = null
): DeephysComputeInput<List<NeuronWithActivation>>(), TopNeuronsCalcType {

  /*small possibility of memory leaks when images is empty, but this is still way better than before*/
  override val cacheManager get() = images.firstOrNull()?.testLoader?.testRAMCache ?: FakeCacheManager

  override fun timedCompute(): List<NeuronWithActivation> {
	if (images.isEmpty() && denomTest == null) return listOf()

	val neurons = forcedNeuronIndices?.let {
	  it.map { layer.neurons[it] }
	} ?: layer.neurons

	val neuronsWithActs = neurons.map { neuron ->
	  val act = if (denomTest != null) ActivationRatioCalc(
		numTest = test,
		denomTest = denomTest,
		neuron = neuron,
		images = images
	  )()
	  else if (normalized) NormalizedAverageActivation(neuron, images, test)()
	  else neuron.averageActivation(images)
	  NeuronWithActivation(neuron, act)
	}

	return if (forcedNeuronIndices == null) {
	  neuronsWithActs.filterNot { it.activation.value.isNaN() }
		.sortedBy { it.activation.value }
		.reversed()
		.take(NUM_TOP_NEURONS)
	} else {
	  neuronsWithActs
	}


  }
}


data class ActivationRatioCalc(
  val numTest: TestOrLoader,
  private val images: Contents<DeephyImage>,
  val denomTest: TestOrLoader,
  private val neuron: InterTestNeuron
): DeephysComputeInput<Activation<*>>() {

  override val cacheManager get() = numTest.testRAMCache /*could be either one.*/ /*small possibility for memory leaks if the user gets keeps swapping out denomTest without swapping out numTest, but this is still a WAY better mechanism than before*/

  companion object {
	const val technique =
	  "This value is the ratio between the maximum activation of this neuron and the maximum activation of the bound neuron"
  }

  /*TODO: make this a lazy val so I don't need to make params above vals*/
  override fun timedCompute() = if (images.isEmpty()) {
	if (numTest==denomTest) AlwaysOneActivation
	else ActivationRatio(numTest.test.maxActivations[neuron]/denomTest.test.maxActivations[neuron])
  } else {
	ActivationRatio(neuron.averageActivation(images).value/denomTest.test.maxActivations[neuron])
  }

}

data class ImageSoftMaxDenom(
  private val image: DeephyImage,
  private val testLoader: TestLoader
): DeephysComputeInput<Float>() {

  override val cacheManager get() = testLoader.testRAMCache

  override fun timedCompute(): Float {
	val clsLay = testLoader.model.classificationLayer
	val preds = image.activationsFor(clsLay.interTest)
	return preds.sumOf { exp(it) }
  }
}

data class ImageTopPredictions(
  private val image: DeephyImage,
  private val testLoader: TestLoader
): DeephysComputeInput<List<Pair<Category, Float>>>() {

  override val cacheManager get() = testLoader.testRAMCache

  override fun timedCompute(): List<Pair<Category, Float>> {
	val clsLay = testLoader.model.classificationLayer
	val preds = image.activationsFor(clsLay.interTest)
	val softMaxDenom = ImageSoftMaxDenom(image, testLoader)()
	return preds.withIndex().sortedBy { it.value }.reversed().take(5).map { thePred ->
	  val exactPred = (exp(thePred.value)/softMaxDenom)
	  val theCategory = testLoader.category(thePred.index)
	  theCategory to exactPred
	}
  }
}

data class CategoryAccuracy(
  private val category: Category,
  private val testLoader: TestLoader
): DeephysComputeInput<Double>() {

  override val cacheManager get() = testLoader.testRAMCache

  override fun timedCompute(): Double {
	val r = testLoader.awaitFinishedTest().imagesWithGroundTruth(category).map {
	  if (testLoader.awaitFinishedTest().preds.await()[it] == category) 1.0 else 0.0
	}.let { it.sum()/it.size }

	return r
  }
}

data class CategoryFalsePositivesSorted(
  private val category: Category,
  private val testLoader: TestLoader
): DeephysComputeInput<List<DeephyImage>>() {

  override val cacheManager get() = testLoader.testRAMCache

  companion object {
	const val blurb =
	  "false positives sorted so that the images with the highest prediction value (after softmax) are first"
  }

  override fun timedCompute(): List<DeephyImage> =
	testLoader.awaitFinishedTest().imagesWithoutGroundTruth(category)
	  .map {
		it to testLoader.awaitFinishedTest().preds.await()[it]
	  }.filter {
		it.second == category
	  }.map {
		it.first to ImageTopPredictions(it.first, testLoader)().first()
	  }.sortedBy {
		it.second.second
	  }.reversed().map {
		it.first
	  }
}

data class CategoryFalseNegativesSorted(
  private val category: Category,
  private val testLoader: TestLoader
): DeephysComputeInput<List<DeephyImage>>() {

  override val cacheManager get() = testLoader.testRAMCache

  companion object {
	const val blurb =
	  "false negatives sorted so that the images with the highest prediction value (after softmax) are first"
  }

  override fun timedCompute() = testLoader.awaitFinishedTest().imagesWithGroundTruth(category)
	.map {
	  it to testLoader.awaitFinishedTest().preds.await()[it]
	}.filter {
	  it.second != category
	}.map {
	  it.first to ImageTopPredictions(it.first, testLoader)().first()
	}.sortedBy {
	  it.second.second
	}.reversed().map {
	  it.first
	}
}


abstract class DeephysComputeInput<O>(): TimedComputeInput<O>() {
  override val stopwatchEnabled get() = DeephySettings.verboseLogging.value
}