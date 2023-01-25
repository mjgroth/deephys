package matt.nn.deephys.model.importformat.neuron

import kotlinx.serialization.Serializable
import matt.nn.deephys.load.cache.RAFCaches
import matt.nn.deephys.load.cache.raf.EvenlySizedRAFCache
import matt.nn.deephys.load.test.dtype.ArrayWrapper
import matt.nn.deephys.load.test.dtype.DType
import java.nio.ByteBuffer

@Serializable class Neuron

class TestNeuron<A: Number>(
  val index: Int,
  val layerIndex: Int,
  activationsRAF: EvenlySizedRAFCache,
  numIms: Int,
  dType: DType<A>
): RAFCaches() {
  val activations = object: CachedRAFProp<ArrayWrapper<A>>(activationsRAF) {
	override fun decode(bytes: ByteArray): ArrayWrapper<A> {

	  return dType.bytesToArray(bytes, numIms)


	}
  }
}