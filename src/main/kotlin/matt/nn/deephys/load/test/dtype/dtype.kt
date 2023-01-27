package matt.nn.deephys.load.test.dtype

import kotlinx.serialization.Serializable
import matt.collect.set.contents.Contents
import matt.collect.set.contents.contentsOf
import matt.lang.List2D
import matt.lang.lastIndex
import matt.math.mat.argmaxn.argmaxn2
import matt.math.reduce.mean
import matt.math.reduce.sumOf
import matt.nn.deephys.calc.TopNeurons
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
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.im.ImageActivationCborBytes
import matt.nn.deephys.model.importformat.im.ImageActivationCborBytesFloat32
import matt.nn.deephys.model.importformat.im.ImageActivationCborBytesFloat64
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.prim.double.DOUBLE_BYTE_LEN
import matt.prim.float.FLOAT_BYTE_LEN
import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.MultiArray
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import java.nio.ByteBuffer


@Serializable
sealed interface DType<N: Number> {
  companion object {
	fun leastPrecise(type: DType<*>, vararg types: DType<*>): DType<*> {
	  return if (types.isEmpty()) type
	  else {
		val all = setOf(type, *types)
		if (Float32 in all) Float32
		else Float64
	  }
	}
  }

  val byteLen: Int
  fun bytesThing(bytes: ByteArray): ImageActivationCborBytes<N>
  fun bytesToArray(bytes: ByteArray, numIms: Int): ArrayWrapper<N>
  fun rawActivation(act: N): RawActivation<N, *>
  fun normalActivation(act: N): NormalActivation<N, *>
  fun activationRatio(act: N): ActivationRatio<N, *>
  fun alwaysOneActivation(): AlwaysOneActivation<N, *>
  fun wrap(multiArray: MultiArray<N, D1>): MultiArrayWrapper<N>
  fun mean(list: List<N>): N
  fun div(num: N, denom: N): N
  fun d1array(list: List<N>): NDArray<N, D1>
  fun d2array(list: List2D<N>): NDArray<N, D2>
  fun exp(v: N): N
  fun sum(list: List<N>): N

  val emptyImageContents: Contents<DeephyImage<N>>


  val label: String





}




@Suppress("UNCHECKED_CAST") fun <N: Number> DType<N>.topNeurons(
  images: Contents<DeephyImage<*>>,
  layer: InterTestLayer,
  test: TypedTestLike<*>,
  denomTest: TypedTestLike<*>?,
  normalized: Boolean,
  forcedNeuronIndices: List<Int>? = null
) = TopNeurons(
  images = images as Contents<DeephyImage<N>>,
  layer = layer,
  test = test as TypedTestLike<N>,
  denomTest = denomTest as TypedTestLike<N>?,
  normalized = normalized,
  forcedNeuronIndices = forcedNeuronIndices
)

sealed class DtypeBase<N: Number>(): DType<N> {
  override val emptyImageContents by lazy { contentsOf<DeephyImage<N>>() }
}

@Serializable
object Float32: DtypeBase<Float>() {

  override val label = "float32"

  override val byteLen = FLOAT_BYTE_LEN
  override fun bytesThing(bytes: ByteArray) = ImageActivationCborBytesFloat32(bytes)
  override fun bytesToArray(bytes: ByteArray, numIms: Int): FloatArrayWrapper {
	return FloatArrayWrapper(FloatArray(numIms).also {
	  ByteBuffer.wrap(bytes).asFloatBuffer().get(it)
	})
  }

  override fun rawActivation(act: Float) = RawActivationFloat32(act)
  override fun activationRatio(act: Float) = ActivationRatioFloat32(act)
  override fun normalActivation(act: Float) = NormalActivationFloat32(act)
  override fun alwaysOneActivation() = AlwaysOneActivationFloat32
  override fun wrap(multiArray: MultiArray<Float, D1>) = FloatMultiArrayWrapper(multiArray)
  override fun mean(list: List<Float>) = list.mean()
  override fun div(num: Float, denom: Float): Float {
	return num/denom
  }

  override fun d1array(list: List<Float>) = list.toNDArray()
  override fun d2array(list: List2D<Float>) = list.toNDArray()
  override fun exp(v: Float) = kotlin.math.exp(v)
  override fun sum(list: List<Float>) = list.sumOf { it }
}

@Serializable
object Float64: DtypeBase<Double>() {
  override val label = "float64"
  override val byteLen = DOUBLE_BYTE_LEN
  override fun bytesThing(bytes: ByteArray) = ImageActivationCborBytesFloat64(bytes)
  override fun bytesToArray(bytes: ByteArray, numIms: Int): DoubleArrayWrapper {
	return DoubleArrayWrapper(DoubleArray(numIms).also {
	  ByteBuffer.wrap(bytes).asDoubleBuffer().get(it)
	})
  }

  override fun rawActivation(act: Double) = RawActivationFloat64(act)
  override fun activationRatio(act: Double) = ActivationRatioFloat64(act)
  override fun normalActivation(act: Double) = NormalActivationFloat64(act)
  override fun alwaysOneActivation() = AlwaysOneActivationFloat64
  override fun wrap(multiArray: MultiArray<Double, D1>) = DoubleMultiArrayWrapper(multiArray)
  override fun mean(list: List<Double>) = list.mean()
  override fun div(num: Double, denom: Double): Double {
	return num/denom
  }

  override fun d1array(list: List<Double>) = list.toNDArray()
  override fun d2array(list: List2D<Double>) = list.toNDArray()
  override fun exp(v: Double) = kotlin.math.exp(v)
  override fun sum(list: List<Double>) = list.sum()
}


typealias FloatActivationData = List<FloatArrayWrapper>
typealias DoubleActivationData = List<DoubleArrayWrapper>

sealed interface ArrayWrapper<T>: List<T> {
  override operator fun get(index: Int): T
  override val size: Int


  override fun isEmpty(): Boolean {
	return size == 0
  }

  override fun iterator() = object: Iterator<T> {
	private var i = -1
	override fun hasNext(): Boolean {
	  return (i + 1) <= lastIndex
	}

	override fun next(): T {
	  return get(++i)
	}

  }

  override fun listIterator(): ListIterator<T> {
	TODO("Not yet implemented")
  }

  override fun listIterator(index: Int): ListIterator<T> {
	TODO("Not yet implemented")
  }

  override fun subList(fromIndex: Int, toIndex: Int): List<T> {
	TODO("Not yet implemented")
  }

  override fun lastIndexOf(element: T): Int {
	TODO("Not yet implemented")
  }

  override fun indexOf(element: T): Int {
	TODO("Not yet implemented")
  }

  override fun containsAll(elements: Collection<T>): Boolean {
	TODO("Not yet implemented")
  }

}

@Serializable
@JvmInline
value class FloatArrayWrapper(private val v: FloatArray): ArrayWrapper<Float> {
  override fun get(index: Int): Float {
	return v[index]
  }

  override val size get() = v.size


  override fun contains(element: Float): Boolean {
	v.forEach {
	  if (it == element) return true
	}
	return false
  }


}

@Serializable
@JvmInline
value class DoubleArrayWrapper(private val v: DoubleArray): ArrayWrapper<Double> {
  override fun get(index: Int): Double {
	return v[index]
  }

  override val size get() = v.size

  override fun contains(element: Double): Boolean {
	v.forEach {
	  if (it == element) return true
	}
	return false
  }
}


sealed interface MultiArrayWrapper<N: Number> {
  fun argmaxn2(n: Int, skipInfinite: Boolean, skipNaN: Boolean): List<Int>
}

@JvmInline
value class FloatMultiArrayWrapper(val a: MultiArray<Float, D1>): MultiArrayWrapper<Float> {
  override fun argmaxn2(n: Int, skipInfinite: Boolean, skipNaN: Boolean): List<Int> {
	return a.argmaxn2(n, skipInfinite = skipInfinite, skipNaN = skipNaN)
  }
}

@JvmInline
value class DoubleMultiArrayWrapper(val a: MultiArray<Double, D1>): MultiArrayWrapper<Double> {
  override fun argmaxn2(n: Int, skipInfinite: Boolean, skipNaN: Boolean): List<Int> {
	return a.argmaxn2(n, skipInfinite = skipInfinite, skipNaN = skipNaN)
  }
}