package matt.nn.deephys.model.importformat.neuron

import kotlinx.serialization.Serializable
import matt.model.flowlogic.latch.asyncloaded.DelegatedSlot

@Serializable class Neuron

class TestNeuron(val index: Int, val layerIndex: Int) {
  val activations = DelegatedSlot<FloatArray>()
}