package matt.nn.deephys.model.data

import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.LayerLike
import matt.nn.deephys.model.importformat.DeephyImage
import matt.nn.deephys.model.importformat.TestOrLoader


data class InterTestLayer(
  val index: Int, override val layerID: String, val neuronCount: Int
): LayerLike {
  val neurons get() = (0 until neuronCount).map { InterTestNeuron(this, it) }
  override val isClassification get() = layerID == "classification"
  override fun toString() = layerID
}

data class InterTestNeuron(
  val layer: InterTestLayer, val index: Int
) {
  fun activation(image: DeephyImage) = image.activationFor(this)
  fun averageActivation(category: Category, testLoader: TestLoader) = category.averageActivationFor(this, testLoader)
  fun averageActivation(images: Set<DeephyImage>) = RawActivation(images.map { activation(it).value }.average().toFloat())
}




@JvmInline value class ImageIndex(val index: Int)

sealed interface CategorySelection {
  val title: String
  val primaryCategory: Category
  val allCategories: Sequence<Category>
}

data class Category(val id: Int, val label: String): CategorySelection {
  override val title = label
  override val primaryCategory = this
  override val allCategories get() = sequence { yield(this@Category) }


  fun averageActivationFor(neuron: InterTestNeuron, testLoader: TestOrLoader): RawActivation {
	return RawActivation(
	  testLoader.test.imagesWithGroundTruth(this).map { it.activationFor(neuron).value }.average().toFloat()
	)
  }

}

data class CategoryConfusion(val first: Category, val second: Category): CategorySelection {
  override val title = "Category Confusion\n\t-${first.label}\n\t-${second.label}"
  override val primaryCategory = first
  override val allCategories get() = sequence { yield(first); yield(second) }
}

