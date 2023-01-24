package matt.nn.deephys.load.test.dtype

import kotlinx.serialization.Serializable
import matt.prim.double.DOUBLE_BYTE_LEN
import matt.prim.float.FLOAT_BYTE_LEN

@Serializable enum class DType(val byteLen: Int) {
  float32(FLOAT_BYTE_LEN), float64(DOUBLE_BYTE_LEN)
}


typealias FloatActivationData = List<FloatArray>
typealias DoubleActivationData = List<DoubleArray>