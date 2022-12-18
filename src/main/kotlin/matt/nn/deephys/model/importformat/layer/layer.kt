package matt.nn.deephys.model.importformat.layer

import kotlinx.serialization.Serializable
import matt.log.tab
import matt.nn.deephys.model.LayerLike
import matt.nn.deephys.model.importformat.neuron.Neuron

@Serializable class Layer(
  override val layerID: String, val neurons: List<Neuron>
): LayerLike {
  override fun toString() = layerID

}