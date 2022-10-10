package matt.nn.deephys.model.data

import matt.nn.deephys.model.LayerLike
import matt.nn.deephys.model.importformat.DeephyImage


data class InterTestLayer(
  val index: Int,
  override val layerID: String,
  val neuronCount: Int
): LayerLike {
  val neurons get() = (0 until neuronCount).map { InterTestNeuron(this, it) }
  override val isClassification get() = layerID == "classification"
  override fun toString() = layerID
}

data class InterTestNeuron(
  val layer: InterTestLayer,
  val index: Int
) {
  fun activation(image: DeephyImage) = image.activationFor(this)
}

@JvmInline
value class ImageIndex(val index: Int)

data class Category(val id: Int, val label: String)