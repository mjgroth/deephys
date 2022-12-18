package matt.nn.deephys.model.importformat.mstate

import matt.model.flowlogic.latch.asyncloaded.DelegatedSlot
import matt.nn.deephys.load.test.ActivationData

class ModelState {
  val activations = DelegatedSlot<ActivationData>()
}


