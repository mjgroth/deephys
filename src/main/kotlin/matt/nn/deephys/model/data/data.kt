package matt.nn.deephys.model.data

import matt.model.op.convert.StringConverter
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.model.LayerLike
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TestOrLoader
import matt.nn.deephys.model.importformat.testlike.TypedTestLike


data class InterTestLayer(
  val index: Int, override val layerID: String, val neuronCount: Int
): LayerLike {
  @OptIn(ExperimentalStdlibApi::class) val neurons get() = (0..<neuronCount).map { InterTestNeuron(this, it) }
  override fun isClassification(model: Model) = layerID == model.classification_layer
  override fun toString() = layerID
}

data class InterTestNeuron(
  val layer: InterTestLayer, val index: Int
) {

  companion object {
	fun stringConverterThatFallsBackToFirst(neurons: List<InterTestNeuron>) = object: StringConverter<InterTestNeuron> {
	  override fun toString(t: InterTestNeuron): String = "${t.index}"
	  override fun fromString(s: String): InterTestNeuron =
		s.toIntOrNull()?.let { i -> neurons.firstOrNull { it.index == i } } ?: neurons.first()
	}
  }

  fun <A: Number> activation(image: DeephyImage<A>) = image.activationFor(this)
  fun averageActivation(category: Category, testLoader: TypedTestLike<*>) =
	category.averageActivationFor(this, testLoader)

  fun <A: Number> averageActivation(
	images: Set<DeephyImage<A>>,
	dType: DType<A>
  ) = run {

	val list = images.map { activation(it).value }

	val m = dType.mean(list)
	/*
		RawActivation(images.map
		{ activation(it).value }.average().toFloat()
		)*/

	dType.rawActivation(
	  m
	)
  }


}


@JvmInline value class ImageIndex(val index: Int)

sealed interface CategorySelection {
  val title: String
  val primaryCategory: Category
  val allCategories: Sequence<Category>
  fun forTest(test: TestOrLoader): CategorySelection
}

data class Category(val id: Int, val label: String): CategorySelection {
  override val title = label
  override val primaryCategory = this
  override val allCategories get() = sequence { yield(this@Category) }


  fun <A: Number> averageActivationFor(neuron: InterTestNeuron, testLoader: TypedTestLike<A>): RawActivation<A, *> {
	val acts = testLoader.test.imagesWithGroundTruth(this).map {
	  it.activationFor(neuron).value
	}


	return testLoader.dtype.rawActivation(
	  testLoader.dtype.mean(acts)
	  /*acts.average().toFloat()*/
	)
  }

  override fun forTest(test: TestOrLoader): Category {
	return test.test.category(id).also {
	  require(it.label == label) {
		"label of category $id of other test doesn't match (${it.label}!=${label})"
	  }
	}
  }

}

data class CategoryConfusion(val first: Category, val second: Category): CategorySelection {
  override val title = "Category Confusion\n\t-${first.label}\n\t-${second.label}"
  override val primaryCategory = first
  override val allCategories get() = sequence { yield(first); yield(second) }
  override fun forTest(test: TestOrLoader): CategoryConfusion {
	val firstOther = first.forTest(test)
	val secondOther = second.forTest(test)
	return CategoryConfusion(first = firstOther, second = secondOther)
  }
}

