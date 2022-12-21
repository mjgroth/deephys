package matt.nn.deephys.model.importformat.neuron

import kotlinx.serialization.Serializable
import matt.nn.deephys.load.cache.RAFCaches
import matt.nn.deephys.load.cache.raf.EvenlySizedRAFCache
import java.nio.ByteBuffer

@Serializable class Neuron

class TestNeuron(
  val index: Int,
  val layerIndex: Int,
  activationsRAF: EvenlySizedRAFCache,
  numIms: Int
): RAFCaches() {
  val activations = object: CachedRAFProp<FloatArray>(activationsRAF) {
	override fun decode(bytes: ByteArray): FloatArray {
	  return FloatArray(numIms).also {
		ByteBuffer.wrap(bytes).asFloatBuffer().get(it)
	  }
	}
  }
}