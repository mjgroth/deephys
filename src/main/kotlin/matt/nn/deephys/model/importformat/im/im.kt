package matt.nn.deephys.model.importformat.im

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.asReadOnlyByteBuffer
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import matt.cbor.read.major.array.ArrayReader
import matt.cbor.read.major.bytestr.ByteStringReader
import matt.cbor.read.streamman.cborReader
import matt.color.rgb
import matt.compose.graphics.color.toComposeColor
import matt.file.raf.cache.EvenlySizedRAFCache
import matt.lang.anno.Open
import matt.lang.anno.PhaseOut
import matt.nn.deephys.load.async.AsyncLoader.DirectLoadedOrFailedValueSlot
import matt.nn.deephys.load.cache.RAFCaches
import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.dtype.DoubleActivationData
import matt.nn.deephys.load.test.dtype.FloatActivationData
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.prim.bytestr.bs.plus
import matt.prim.bytestr.bs.withIndex
import matt.prim.converters.StringConverter
import matt.prim.j.bs.readAndCopyDoubles
import matt.prim.j.bs.readAndCopyFloats
import matt.prim.pdouble.DOUBLE_BYTE_LEN
import matt.prim.pfloat.FLOAT_BYTE_LEN
import matt.prim.weak.common.lazyWeak
import matt.prim.weak.weak
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

class DeephyImage<A : Number>(
    val imageID: Int,
    categoryID: Int,
    category: String,
    val testLoader: TypedTestLike<A>,
    val index: Int,
    val model: Model,
    val features: Map<String, String>?,
    test: DirectLoadedOrFailedValueSlot<Test<A>>,
    activationsRAF: EvenlySizedRAFCache,
    pixelsRAF: EvenlySizedRAFCache,
    dtype: DType<A> /*just for generic*/
) : RAFCaches(), Comparable<DeephyImage<A>> {

    override fun compareTo(other: DeephyImage<A>): Int = imageID.compareTo(other.imageID)

    companion object {
        fun <A: Number> stringConverterThatFallsBackToFirst(images: List<DeephyImage<A>>) =
            object : StringConverter<DeephyImage<A>> {
                override fun toString(t: DeephyImage<A>): String = "${t.index}"
                override fun fromString(s: String): DeephyImage<A> =
                    s.toIntOrNull()?.let { i -> images.firstOrNull { it.index == i } } ?: images.first()
            }
    }

    override fun toString(): String = "[Deephy Image with ID=$imageID]"

    val weak by lazy { weak(this) }

    val category = Category(id = categoryID, label = category)

    /*totally guessing. This might actually be the height.*/
    @Suppress("unused")
    val widthMaybe by lazy {
        matrix[0].size.toDouble()
    }

    val matrix by lazyWeak {
        val d = data.await()
        val numRows = d[0].size
        val numCols = d[0][0].size

        (0 until numRows).map { index1 ->
            MutableList(numCols) { index2 ->
                rgb(d[0][index1][index2], d[1][index1][index2], d[2][index1][index2]).toComposeColor()
            }
        }
    }

    val activations =
        object : CachedRAFProp<List<List<A>>>(activationsRAF) {
            override fun decode(bytes: ByteString): List<List<A>> {
                val byteThing = dtype.bytesThing(bytes)
                return byteThing.parse2DArray()
            }
        }

    internal val weakActivations by lazyWeak {
        activations.await()
    }

    fun activationsFor(rLayer: InterTestLayer): List<A> = weakActivations[rLayer.index]
    fun activationFor(neuron: InterTestNeuron) = dtype.rawActivation(weakActivations[neuron.layer.index][neuron.index])

    val data =
        object : CachedRAFProp<PixelData3>(pixelsRAF) {
            override fun decode(bytes: ByteString): PixelData3 = readPixels(bytes)
        }

    @PhaseOut
    private val weakTest = WeakReference(test)

    val prediction by lazy {
        weakTest.get()!!.awaitRequireSuccessful().preds.await()[this]!!
    }

    val dtype get() = weakTest.get()!!.awaitRequireSuccessful().dtype
}

typealias PixelData2 = List<IntArray>
typealias PixelData3 = List<PixelData2>

fun ArrayReader.readPixels(): PixelData3 =
    readEachManually<ArrayReader, PixelData2> {
        readEachManually<ByteStringReader, IntArray> {
            val r = IntArray(count.toInt())
            for ((i, b) in read().raw.withIndex()) r[i] = b.toInt() and 0xff
            r
        }
    }

fun readPixels(cborPixelBytes3d: ByteString): PixelData3 =
    cborPixelBytes3d.cborReader().readManually<ArrayReader, PixelData3> {
        readPixels()
    }

fun ArrayReader.readFloatActivations() =
    readEachManually<ByteStringReader, List<Float>> {
        val r = read().raw.readAndCopyFloats(count = count.toInt() / FLOAT_BYTE_LEN)
        r.asList()
    }

fun ArrayReader.readDoubleActivations() =
    readEachManually<ByteStringReader, List<Double>> {
        val r = read().raw.readAndCopyDoubles(count = count.toInt() / DOUBLE_BYTE_LEN)
        r.asList()
    }

sealed interface ImageActivationCborBytes<A : Number> {
    val bytes: ByteString
    fun parse2DArray(): List<List<A>>
    @Open
    fun rawBytes() =
        bytes.cborReader().readManually<ArrayReader, ByteString> {
            readEachManually<ByteStringReader, ByteString> {
                read().raw
            }.reduce { acc, bytes -> acc + bytes }
        }

    fun dtypeByteReadyBufferFlow(): Flow<ByteBuffer>
}

@JvmInline
value class ImageActivationCborBytesFloat32(override val bytes: ByteString) : ImageActivationCborBytes<Float> {

    override fun parse2DArray(): FloatActivationData =
        bytes.cborReader().readManually<ArrayReader, FloatActivationData> {
            readFloatActivations()
        }

    override fun dtypeByteReadyBufferFlow(): Flow<ByteBuffer> =
        flow {
            bytes.cborReader().readManuallySuspending<ArrayReader, Unit> {
                val _ =
                    readEachManuallySuspending<ByteStringReader, Unit> {
                        val buffer = read().raw.asReadOnlyByteBuffer()
                        (FLOAT_BYTE_LEN until buffer.capacity() step FLOAT_BYTE_LEN).forEach {
                            buffer.limit(it)
                            emit(buffer)
                        }
                    }
            }
        }
}

@JvmInline
value class ImageActivationCborBytesFloat64(override val bytes: ByteString) : ImageActivationCborBytes<Double> {
    override fun parse2DArray(): DoubleActivationData =
        bytes.cborReader().readManually<ArrayReader, DoubleActivationData> {
            readDoubleActivations()
        }

    @OptIn(UnsafeByteStringApi::class)
    override fun dtypeByteReadyBufferFlow(): Flow<ByteBuffer> =
        flow {
            bytes.cborReader().readManuallySuspending<ArrayReader, Unit> {
                val _ =
                    readEachManuallySuspending<ByteStringReader, Unit> {
                        UnsafeByteStringOperations.withByteArrayUnsafe(read().raw) { rawBytes ->
                            val buffer = ByteBuffer.wrap(rawBytes)
                            (DOUBLE_BYTE_LEN until buffer.capacity() step DOUBLE_BYTE_LEN).forEach {
                                buffer.limit(it)
                                emit(buffer)
                            }
                        }
                    }
            }
        }
}
