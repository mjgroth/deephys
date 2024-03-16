package matt.nn.deephys.load.test.dtype

import kotlinx.serialization.Serializable
import matt.collect.set.contents.Contents
import matt.collect.set.contents.contentsOf
import matt.lang.cast.Caster
import matt.lang.common.List2D
import matt.math.arithmetic.sumOf
import matt.math.numalg.matalg.multiarray.DoubleMultiArrayWrapper
import matt.math.numalg.matalg.multiarray.FloatMultiArrayWrapper
import matt.math.numalg.matalg.multiarray.MultiArrayWrapper
import matt.math.statistics.mean
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.calc.act.ActivationRatio
import matt.nn.deephys.calc.act.ActivationRatioFloat32
import matt.nn.deephys.calc.act.ActivationRatioFloat64
import matt.nn.deephys.calc.act.AlwaysOneActivation
import matt.nn.deephys.calc.act.AlwaysOneActivationFloat32
import matt.nn.deephys.calc.act.AlwaysOneActivationFloat64
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.calc.act.RawActivationFloat32
import matt.nn.deephys.calc.act.RawActivationFloat64
import matt.nn.deephys.gui.fix.TestAndSomeImages
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
import kotlin.math.exp as kotlinExp


@Serializable
sealed interface DType<N : Number>: Caster<N> {
    companion object {
        fun leastPrecise(
            type: DType<*>,
            vararg types: DType<*>
        ): DType<*> =
            if (types.isEmpty()) type
            else {
                val all = setOf(type, *types)
                if (Float32 in all) Float32
                else Float64
            }
    }

    val byteLen: Int
    fun bytesThing(bytes: ByteArray): ImageActivationCborBytes<N>
    fun bytesToArray(
        bytes: ByteArray,
        numIms: Int
    ): List<N>

    fun rawActivation(act: N): RawActivation<N, *>

    fun activationRatio(act: N): ActivationRatio<N, *>
    fun alwaysOneActivation(): AlwaysOneActivation<N, *>
    fun wrap(multiArray: MultiArray<N, D1>): MultiArrayWrapper<N>
    fun mean(list: List<N>): N
    fun div(
        num: N,
        denom: N
    ): N

    fun d1array(list: List<N>): NDArray<N, D1>
    fun d2array(list: List2D<N>): NDArray<N, D2>
    fun exp(v: N): N
    fun sum(list: List<N>): N
    val emptyImageContents: Contents<DeephyImage<N>>
    val label: String
    val one: N
    val zero: N
}


fun <N : Number> topNeurons(
    testAndImages: TestAndSomeImages<N>,
    layer: InterTestLayer,
    denomTest: TypedTestLike<*>?,
    forcedNeuronIndices: List<Int>? = null
) = TopNeurons(
    testAndImages = testAndImages,
    layer = layer,
    denomTest = denomTest,
    forcedNeuronIndices = forcedNeuronIndices
)

sealed class DtypeBase<N : Number>() : DType<N> {
    final override val emptyImageContents by lazy { contentsOf<DeephyImage<N>>() }
}

@Serializable
object Float32 : DtypeBase<Float>() {

    override fun cast(a: Any?): Float = a as Float

    override val label = "float32"

    override val byteLen = FLOAT_BYTE_LEN
    override fun bytesThing(bytes: ByteArray) = ImageActivationCborBytesFloat32(bytes)
    override fun bytesToArray(
        bytes: ByteArray,
        numIms: Int
    ): List<Float> =
        FloatArray(numIms).also {
            ByteBuffer.wrap(bytes).asFloatBuffer().get(it)
        }.asList()

    override fun rawActivation(act: Float) = RawActivationFloat32(act)
    override fun activationRatio(act: Float) = ActivationRatioFloat32(act)

    override fun alwaysOneActivation() = AlwaysOneActivationFloat32
    override fun wrap(multiArray: MultiArray<Float, D1>) = FloatMultiArrayWrapper(multiArray)
    override fun mean(list: List<Float>) = list.mean()
    override fun div(
        num: Float,
        denom: Float
    ): Float = num / denom

    override fun d1array(list: List<Float>) = list.toNDArray()
    override fun d2array(list: List2D<Float>) = list.toNDArray()
    override fun exp(v: Float) = kotlinExp(v)
    override fun sum(list: List<Float>) = list.sumOf { it }
    override val one = 1f
    override val zero = 0f
}

@Serializable
object Float64 : DtypeBase<Double>() {

    override fun cast(a: Any?): Double = a as Double
    override val label = "float64"
    override val byteLen = DOUBLE_BYTE_LEN
    override fun bytesThing(bytes: ByteArray) = ImageActivationCborBytesFloat64(bytes)
    override fun bytesToArray(
        bytes: ByteArray,
        numIms: Int
    ): List<Double> =
        DoubleArray(numIms).also {
            ByteBuffer.wrap(bytes).asDoubleBuffer().get(it)
        }.asList()

    override fun rawActivation(act: Double) = RawActivationFloat64(act)
    override fun activationRatio(act: Double) = ActivationRatioFloat64(act)

    override fun alwaysOneActivation() = AlwaysOneActivationFloat64
    override fun wrap(multiArray: MultiArray<Double, D1>) = DoubleMultiArrayWrapper(multiArray)
    override fun mean(list: List<Double>) = list.mean()
    override fun div(
        num: Double,
        denom: Double
    ): Double = num / denom

    override fun d1array(list: List<Double>) = list.toNDArray()
    override fun d2array(list: List2D<Double>) = list.toNDArray()
    override fun exp(v: Double) = kotlinExp(v)
    override fun sum(list: List<Double>) = list.sum()
    override val one = 1.0
    override val zero = 0.0
}


typealias FloatActivationData = List<List<Float>>
typealias DoubleActivationData = List<List<Double>>


