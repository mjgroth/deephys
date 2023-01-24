package matt.nn.deephys.load.test.dtype

import kotlinx.serialization.Serializable
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.calc.act.RawActivationFloat32
import matt.nn.deephys.calc.act.RawActivationFloat64
import matt.nn.deephys.model.importformat.im.ImageActivationCborBytes
import matt.nn.deephys.model.importformat.im.ImageActivationCborBytesFloat32
import matt.nn.deephys.model.importformat.im.ImageActivationCborBytesFloat64
import matt.prim.double.DOUBLE_BYTE_LEN
import matt.prim.float.FLOAT_BYTE_LEN

@Serializable
sealed interface DType<N: Number> {
  val byteLen: Int
  fun bytesThing(bytes: ByteArray): ImageActivationCborBytes<N>
  fun rawActivation(act: N): RawActivation<N, *>
}

@Serializable
object Float32: DType<Float> {
  override val byteLen = FLOAT_BYTE_LEN
  override fun bytesThing(bytes: ByteArray) = ImageActivationCborBytesFloat32(bytes)
  override fun rawActivation(act: Float) = RawActivationFloat32(act)
}

@Serializable
object Float64: DType<Double> {
  override val byteLen = DOUBLE_BYTE_LEN
  override fun bytesThing(bytes: ByteArray) = ImageActivationCborBytesFloat64(bytes)
  override fun rawActivation(act: Double) = RawActivationFloat64(act)
}


typealias FloatActivationData = List<FloatArrayWrapper>
typealias DoubleActivationData = List<DoubleArrayWrapper>

sealed interface ArrayWrapper<T> {
  operator fun get(index: Int): T
  val size: Int
}

@Serializable
@JvmInline
value class FloatArrayWrapper(private val v: FloatArray): ArrayWrapper<Float> {
  override fun get(index: Int): Float {
	return v[index]
  }

  override val size get() = v.size
}

@Serializable
@JvmInline
value class DoubleArrayWrapper(private val v: DoubleArray): ArrayWrapper<Double> {
  override fun get(index: Int): Double {
	return v[index]
  }
  override val size get() = v.size
}

