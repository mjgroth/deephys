package matt.nn.deephy.model

import matt.nn.deephy.model.data.InterTestLayer
import matt.nn.deephy.model.data.InterTestNeuron
import matt.nn.deephy.model.importformat.Layer
import matt.nn.deephy.model.importformat.Model


interface LayerLike {
  val layerID: String
  override fun toString(): String
  val isClassification get() = layerID == "classification"
}


class ResolvedLayer(
  layer: Layer,
  val model: Model,
  val index: Int
): LayerLike by layer {
  val neurons: List<ResolvedNeuron> = List(layer.neurons.size) { index ->
	ResolvedNeuron(
	  index = index,
	  layer = this@ResolvedLayer
	)
  }
  val interTest by lazy { InterTestLayer(index, layerID = layer.layerID, neuronCount = neurons.size) }
}


interface ResolvedNeuronLike {
  val index: Int
  val layer: ResolvedLayer
  val interTest: InterTestNeuron
}


class ResolvedNeuron(
  override val index: Int,
  override val layer: ResolvedLayer,
): ResolvedNeuronLike {
  override val interTest by lazy { InterTestNeuron(layer.interTest, index) }
}