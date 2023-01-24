package matt.nn.deephys.load.test.dtype

import kotlinx.serialization.Serializable
import matt.nn.deephys.calc.act.ActivationRatio
import matt.nn.deephys.calc.act.ActivationRatioFloat32
import matt.nn.deephys.calc.act.ActivationRatioFloat64
import matt.nn.deephys.calc.act.AlwaysOneActivation
import matt.nn.deephys.calc.act.AlwaysOneActivationFloat32
import matt.nn.deephys.calc.act.AlwaysOneActivationFloat64
import matt.nn.deephys.calc.act.NormalActivation
import matt.nn.deephys.calc.act.NormalActivationFloat32
import matt.nn.deephys.calc.act.NormalActivationFloat64
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
  companion object {
	fun leastPrecise(type: DType<*>, vararg types: DType<*>): DType<*> {
      return if (types.isEmpty()) type
      else {
        val all = setOf(type,*types)
        if (Float32 in types) Float32
        else Float64
      }
	}
  }

  val byteLen: Int
  fun bytesThing(bytes: ByteArray): ImageActivationCborBytes<N>
  fun rawActivation(act: N): RawActivation<N, *>
  fun normalActivation(act: N): NormalActivation<N, *>
  fun activationRatio(act: N): ActivationRatio<N, *>
  fun alwaysOneActivation(): AlwaysOneActivation<N, *>
}

@Serializable
object Float32: DType<Float> {
  override val byteLen = FLOAT_BYTE_LEN
  override fun bytesThing(bytes: ByteArray) = ImageActivationCborBytesFloat32(bytes)
  override fun rawActivation(act: Float) = RawActivationFloat32(act)
  override fun activationRatio(act: Float) = ActivationRatioFloat32(act)
  override fun normalActivation(act: Float) = NormalActivationFloat32(act)
  override fun alwaysOneActivation() = AlwaysOneActivationFloat32
}

@Serializable
object Float64: DType<Double> {
  override val byteLen = DOUBLE_BYTE_LEN
  override fun bytesThing(bytes: ByteArray) = ImageActivationCborBytesFloat64(bytes)
  override fun rawActivation(act: Double) = RawActivationFloat64(act)
  override fun activationRatio(act: Double) = ActivationRatioFloat64(act)
  override fun normalActivation(act: Double) = NormalActivationFloat64(act)
  override fun alwaysOneActivation() = AlwaysOneActivationFloat64
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

