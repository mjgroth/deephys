package matt.nn.deephys.model.importformat.neuron

import kotlinx.serialization.Serializable
import matt.nn.deephys.load.cache.RAFCaches
import matt.nn.deephys.load.cache.raf.EvenlySizedRAFCache
import matt.nn.deephys.load.test.dtype.DType

@Serializable
class Neuron

class TestNeuron<A : Number>(
    val index: Int,
    val layerIndex: Int,
    activationsRAF: EvenlySizedRAFCache,
    numIms: Int,
    dType: DType<A>
) : RAFCaches() {

    override fun toString(): String {
        return "TestNeuron $index of layer $layerIndex"
    }

    val activations = object : CachedRAFProp<List<A>>(activationsRAF) {
        override fun decode(bytes: ByteArray): List<A> {

            return dType.bytesToArray(bytes, numIms)


        }
    }
}