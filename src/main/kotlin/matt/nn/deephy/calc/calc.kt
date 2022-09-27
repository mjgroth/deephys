package matt.nn.deephy.calc

import matt.math.jmath.sigFigs
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.InterTestNeuron

class ActivationRatio(
  private val numTest: TestLoader,
  private val denomTest: TestLoader,
  private val neuron: InterTestNeuron
) {

  companion object {
	val technique = "This value is the ratio between the maximum activation of this neuron and the maximum activation of the bound neuron"
  }

  val result by lazy {
	numTest.awaitFinishdTest().maxActivations[neuron]/denomTest.awaitFinishedTest().maxActivations[neuron]
  }
  val formattedResult by lazy {
	" %=${result.sigFigs(3)}"
  }
}