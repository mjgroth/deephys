package matt.nn.deephy.calc

import matt.caching.compcache.ComputeInput
import matt.math.jmath.sigFigs
import matt.math.sumOf
import matt.nn.deephy.gui.global.DeephyText
import matt.nn.deephy.gui.global.deephyTooltip
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.data.Category
import matt.nn.deephy.model.data.ImageIndex
import matt.nn.deephy.model.data.InterTestLayer
import matt.nn.deephy.model.data.InterTestNeuron
import matt.nn.deephy.model.importformat.DeephyImage
import matt.nn.deephy.model.importformat.Test
import kotlin.math.exp


data class NormalizedActivation(
  private val neuron: InterTestNeuron,
  private val image: DeephyImage,
  private val test: Test
): ComputeInput<Float>() {

  companion object {
	const val RAW_ACT_SYMBOL = "Y"
	const val NORMALIZED_ACT_SYMBOL = "Ŷ"
	const val normalizeTopNeuronsBlurb =
	  "Normalized Activation ($NORMALIZED_ACT_SYMBOL) = Raw Activation ($RAW_ACT_SYMBOL) / max(activation for each image for this neuron)"
  }

  override fun compute(): Float {
	return neuron.activation(image)/test.maxActivations[neuron]
  }
}

data class TopImages(
  private val neuron: InterTestNeuron,
  private val test: TestLoader
): ComputeInput<List<ImageIndex>>() {
  override fun compute(): List<ImageIndex> =
	test.awaitFinishedTest().activationsByNeuron[neuron].withIndex().sortedBy { it.value }.reversed()
	  .map { ImageIndex(it.index) }
}

data class TopNeurons(
  private val image: DeephyImage,
  private val layer: InterTestLayer,
  val normalized: Boolean
): ComputeInput<List<InterTestNeuron>>() {
  override fun compute(): List<InterTestNeuron> = layer.neurons.map { it }.sortedBy {
	if (normalized) NormalizedActivation(it, image, image.test.await())() else it.activation(image)
  }.reversed().take(25)
}



class ActivationRatio(
  private val numTest: TestLoader,
  private val denomTest: TestLoader,
  private val neuron: InterTestNeuron
): ComputeInput<Float>() {

  companion object {
	const val technique =
	  "This value is the ratio between the maximum activation of this neuron and the maximum activation of the bound neuron"
  }

  /*TODO: make this a lazy val so I don't need to make params above vals*/
  override fun compute() =
	numTest.awaitFinishedTest().maxActivations[neuron]/denomTest.awaitFinishedTest().maxActivations[neuron]

  val formattedResult by lazy {
	" %=${findOrCompute().sigFigs(3)}"
  }

  fun text() = DeephyText(formattedResult).apply {
	deephyTooltip(technique)
  }
}

class ImageSoftMaxDenom(
  private val image: DeephyImage,
  private val testLoader: TestLoader
): ComputeInput<Float>() {
  override fun compute(): Float {
	val clsLay = testLoader.model.classificationLayer
	val preds = image.activationsFor(clsLay.interTest)
	return preds.sumOf { exp(it) }
  }
}

class ImageTopPredictions(
  private val image: DeephyImage,
  private val testLoader: TestLoader
): ComputeInput<List<Pair<Category, Float>>>() {
  override fun compute(): List<Pair<Category, Float>> {
	val clsLay = testLoader.model.classificationLayer
	val preds = image.activationsFor(clsLay.interTest)
	val softMaxDenom = ImageSoftMaxDenom(image, testLoader)()
	return preds.withIndex().sortedBy { it.value }.reversed().take(5).map { thePred ->
	  val exactPred = (exp(thePred.value)/softMaxDenom)
	  val theCategory = testLoader.category(thePred.index)
	  theCategory to exactPred
	  /* val predClassNameString = theCategory.let {
		 if (", texture :" in it.label) {
		   it.label.substringBefore(",")
		 } else it.label
	   }*/
	}
  }
}

data class CategoryAccuracy(
  private val category: Category,
  private val testLoader: TestLoader
): ComputeInput<Double>() {
  override fun compute() = testLoader
	.awaitFinishedTest()
	.imagesWithGroundTruth(category)
	.map {
	  if (ImageTopPredictions(it, testLoader)().first().first == category) 1.0 else 0.0
	}.let { it.sum()/it.size }
}

data class CategoryFalsePositivesSorted(
  private val category: Category,
  private val testLoader: TestLoader
): ComputeInput<List<DeephyImage>>() {
  companion object {
	const val blurb = "false positives sorted so that the images with the highest prediction value (after softmax) are first"
  }
  override fun compute(): List<DeephyImage> = testLoader
	.awaitFinishedTest()
	.imagesWithoutGroundTruth(category)
	.map {
	  it to ImageTopPredictions(it, testLoader)().first()
	}.filter {
	  it.second.first == category
	}.sortedBy {
	  it.second.second
	}.reversed().map {
	  it.first
	}
}


/*data class MatthewCorrelationCoefficient(
  private val test: TestLoader,
  private val category: Category,
): ComputeInput<Float>() {

  companion object {
	const val SYMBOL = "MCC"
	const val mccBlurb = "Matthew Correlation Coefficient ($SYMBOL) = "
  }

  override fun compute()
}*/
