package matt.nn.deephys.calc

import matt.caching.compcache.timed.TimedComputeInput
import matt.math.jmath.sigFigs
import matt.math.mat.argmaxn.argmaxn2
import matt.math.reduce.sumOf
import matt.nn.deephys.calc.act.Activation
import matt.nn.deephys.calc.act.NormalActivation
import matt.nn.deephys.calc.act.NormalActivation.Companion.NORMALIZED_ACT_SYMBOL
import matt.nn.deephys.calc.act.RawActivation.Companion.RAW_ACT_SYMBOL
import matt.nn.deephys.gui.global.DeephyText
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.ImageIndex
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.DeephyImage
import matt.nn.deephys.state.DeephySettings
import kotlin.math.exp


/*
data class NormalizedActivation(
  private val neuron: InterTestNeuron,
  private val image: DeephyImage,
  private val test: Test
): DeephysComputeInput<NormalActivation>() {



  override fun timedCompute(): NormalActivation {
	return NormalActivation(neuron.activation(image).value/test.maxActivations[neuron])
  }
}
*/


//
//data class NormalizedAverageActivation(
//  private val neuron: InterTestNeuron,
//  private val cat: Category,
//  private val test: TestOrLoader,
//
//  ): DeephysComputeInput<NormalActivation>() {
//  override fun timedCompute(): NormalActivation {
//	return NormalActivation(cat.averageActivationFor(neuron, test).value/test.test.maxActivations[neuron])
//  }
//}


class UniqueContents<E>(set: Set<E>): Set<E> by set {

  constructor(itr: Iterable<E>): this(itr.toSet())
  constructor(itr: Sequence<E>): this(itr.toSet())

  override fun equals(other: Any?): Boolean {
	return other is UniqueContents<*> && containsAll(other)
  }

  override fun hashCode(): Int {
	return map { it.hashCode() }.sum()
  }
}

data class NormalizedAverageActivation(
  private val neuron: InterTestNeuron,
  private val images: UniqueContents<DeephyImage>,
): DeephysComputeInput<NormalActivation>() {

  companion object {
	const val normalizeTopNeuronsBlurb =
	  "Normalized Activation ($NORMALIZED_ACT_SYMBOL) = Raw Activation ($RAW_ACT_SYMBOL) / max(activation for each image for this neuron)"
  }

  override fun timedCompute(): NormalActivation {
	return NormalActivation(neuron.averageActivation(images).value/images.first().test.await().maxActivations[neuron])
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
  private val neuron: InterTestNeuron, private val test: TestLoader, private val num: Int
): DeephysComputeInput<List<ImageIndex>>() {
  override fun timedCompute(): List<ImageIndex> = run {
	val theTest = test.awaitFinishedTest()
	toc("got theTest")
	val acts = theTest.activationsByNeuron[neuron]
	toc("got acts")
	val indices = acts.argmaxn2(num)
	toc("got indices")
	indices.map { ImageIndex(it) }
  }
}

private const val NUM_TOP_NEURONS = 25

interface TopNeuronsCalcType {
  operator fun invoke(): List<NeuronWithActivation>
}

data class NeuronWithActivation(val neuron: InterTestNeuron, val activation: Activation<*>)

data class TopNeurons(
  private val images: UniqueContents<DeephyImage>, private val layer: InterTestLayer, val normalized: Boolean
): DeephysComputeInput<List<NeuronWithActivation>>(), TopNeuronsCalcType {
  override fun timedCompute(): List<NeuronWithActivation> {
	if (images.isEmpty()) return listOf()
	return layer.neurons.map {
	  if (normalized) NeuronWithActivation(
		it, NormalizedAverageActivation(it, images)()
	  ) else NeuronWithActivation(it, it.averageActivation(images))
	}.filterNot { it.activation.value.isNaN() }.sortedBy { it.activation.value }.reversed().take(NUM_TOP_NEURONS)
  }
}

//data class TopNeuronsCategory(
//  private val catSelect: CategorySelection,
//  private val layer: InterTestLayer,
//  val normalized: Boolean,
//  val testLoader: TestLoader
//): DeephysComputeInput<List<NeuronWithActivation>>(), TopNeuronsCalcType {
//  override fun timedCompute(): List<NeuronWithActivation> {
//
//
//	return layer.neurons.map {
//
//	  val v = when (catSelect) {
//		is Category          -> {
//		  if (normalized) NormalizedAverageActivation(it, catSelect, testLoader)()
//		  else it.averageActivation(
//			catSelect, testLoader
//		  )
//
//		}
//
//		is CategoryConfusion -> {
//		  if (normalized) NormalizedAverageActivation(
//			it, catSelect.first, testLoader
//		  )() + NormalizedAverageActivation(
//			it, catSelect.second, testLoader
//		  )()
//		  else it.averageActivation(
//			catSelect.first, testLoader
//		  ) + it.averageActivation(
//			catSelect.second, testLoader
//		  )
//		}
//	  }
//
//
//	  NeuronWithActivation(it, v)
//
//	}.sortedBy { it.activation }.reversed().take(NUM_TOP_NEURONS)
//
//
//  }
//}


class ActivationRatio(
  private val numTest: TestLoader, private val denomTest: TestLoader, private val neuron: InterTestNeuron
): DeephysComputeInput<Float>() {

  companion object {
	const val technique =
	  "This value is the ratio between the maximum activation of this neuron and the maximum activation of the bound neuron"
  }

  /*TODO: make this a lazy val so I don't need to make params above vals*/
  override fun timedCompute() =
	numTest.awaitFinishedTest().maxActivations[neuron]/denomTest.awaitFinishedTest().maxActivations[neuron]

  val formattedResult by lazy {
	" %=${findOrCompute().sigFigs(3)}"
  }

  fun text() = DeephyText(formattedResult).apply {
	deephyTooltip(technique)
  }
}

class ImageSoftMaxDenom(
  private val image: DeephyImage, private val testLoader: TestLoader
): DeephysComputeInput<Float>() {
  override fun timedCompute(): Float {
	val clsLay = testLoader.model.classificationLayer
	val preds = image.activationsFor(clsLay.interTest)
	return preds.sumOf { exp(it) }
  }
}

class ImageTopPredictions(
  private val image: DeephyImage, private val testLoader: TestLoader
): DeephysComputeInput<List<Pair<Category, Float>>>() {
  override fun timedCompute(): List<Pair<Category, Float>> {
	val clsLay = testLoader.model.classificationLayer
	val preds = image.activationsFor(clsLay.interTest)
	val softMaxDenom = ImageSoftMaxDenom(image, testLoader)()
	return preds.withIndex().sortedBy { it.value }.reversed().take(5).map { thePred ->
	  val exactPred = (exp(thePred.value)/softMaxDenom)
	  val theCategory = testLoader.category(thePred.index)
	  theCategory to exactPred    /* val predClassNameString = theCategory.let {
		 if (", texture :" in it.label) {
		   it.label.substringBefore(",")
		 } else it.label
	   }*/
	}
  }
}

data class CategoryAccuracy(
  private val category: Category, private val testLoader: TestLoader
): DeephysComputeInput<Double>() {
  override fun timedCompute() = testLoader.awaitFinishedTest().imagesWithGroundTruth(category).map {
	if (testLoader.awaitFinishedTest().preds.await()[it] == category) 1.0 else 0.0    /*if (ImageTopPredictions(it, testLoader)().first().first == category) 1.0 else 0.0*/
  }.let { it.sum()/it.size }
}

data class CategoryFalsePositivesSorted(
  private val category: Category, private val testLoader: TestLoader
): DeephysComputeInput<List<DeephyImage>>() {
  companion object {
	const val blurb =
	  "false positives sorted so that the images with the highest prediction value (after softmax) are first"
  }

  override fun timedCompute(): List<DeephyImage> = testLoader.awaitFinishedTest().imagesWithoutGroundTruth(category)
	.map {    /*it to ImageTopPredictions(it, testLoader)().first()*/
	  it to testLoader.awaitFinishedTest().preds.await()[it]
	}.filter {
	  it.second/*.first*/ == category
	}.map {
	  it.first to ImageTopPredictions(it.first, testLoader)().first()
	}.sortedBy {
	  it.second.second
	}.reversed().map {
	  it.first
	}
}

data class CategoryFalseNegativesSorted(
  private val category: Category, private val testLoader: TestLoader
): DeephysComputeInput<List<DeephyImage>>() {
  companion object {
	const val blurb =
	  "false negatives sorted so that the images with the highest prediction value (after softmax) are first"
  }

  override fun timedCompute(): List<DeephyImage> = testLoader.awaitFinishedTest().imagesWithGroundTruth(category)
	.map {    /*it to ImageTopPredictions(it, testLoader)().first()*/
	  it to testLoader.awaitFinishedTest().preds.await()[it]
	}.filter {
	  it.second/*.first*/ != category
	}.map {
	  it.first to ImageTopPredictions(it.first, testLoader)().first()
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



abstract class DeephysComputeInput<O>(): TimedComputeInput<O>() {
  override val stopwatchEnabled get() = DeephySettings.verboseLogging.value
}