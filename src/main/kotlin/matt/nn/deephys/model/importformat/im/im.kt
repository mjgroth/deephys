package matt.nn.deephys.model.importformat.im

import matt.cbor.read.major.array.ArrayReader
import matt.cbor.read.major.bytestr.ByteStringReader
import matt.cbor.read.streamman.cborReader
import matt.fx.graphics.wrapper.style.FXColor
import matt.lang.anno.PhaseOut
import matt.lang.weak.MyWeakRef
import matt.lang.weak.lazyWeak
import matt.model.op.convert.StringConverter
import matt.nn.deephys.load.async.AsyncLoader.LoadedOrFailedValueSlot
import matt.nn.deephys.load.cache.RAFCaches
import matt.nn.deephys.load.cache.raf.EvenlySizedRAFCache
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.load.test.dtype.ArrayWrapper
import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.dtype.DoubleActivationData
import matt.nn.deephys.load.test.dtype.DoubleArrayWrapper
import matt.nn.deephys.load.test.dtype.FloatActivationData
import matt.nn.deephys.load.test.dtype.FloatArrayWrapper
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test
import matt.prim.double.DOUBLE_BYTE_LEN
import matt.prim.float.FLOAT_BYTE_LEN
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

class DeephyImage<A: Number>(
  val imageID: Int,
  categoryID: Int,
  category: String,
  val testLoader: TestLoader,
  val index: Int,
  val model: Model,
  val features: Map<String, String>?,
  test: LoadedOrFailedValueSlot<Test<*>>,
  activationsRAF: EvenlySizedRAFCache,
  pixelsRAF: EvenlySizedRAFCache,
  dtype: DType<A> /*just for generic*/
): RAFCaches() {

  companion object {
	fun stringConverterThatFallsBackToFirst(images: List<DeephyImage<*>>) = object: StringConverter<DeephyImage<*>> {
	  override fun toString(t: DeephyImage<*>): String = "${t.index}"
	  override fun fromString(s: String): DeephyImage<*> =
		s.toIntOrNull()?.let { i -> images.firstOrNull { it.index == i } } ?: images.first()
	}
  }

  override fun toString(): String {
	return "[Deephy Image with ID=${imageID}]"
  }

  val weak by lazy { MyWeakRef(this) }


  val category = Category(id = categoryID, label = category)

  /*totally guessing. This might actually be the height.*/
  val widthMaybe by lazy {
	matrix[0].size.toDouble()
  }

  val matrix by lazyWeak {
	val d = data.await()
	val numRows = d[0].size
	val numCols = d[0][0].size

	(0 until numRows).map { index1 ->
	  MutableList<FXColor>(numCols) { index2 ->
		FXColor.rgb(d[0][index1][index2], d[1][index1][index2], d[2][index1][index2])
	  }
	}
  }


  val activations = object: CachedRAFProp<List<ArrayWrapper<A>>>(activationsRAF) {
	override fun decode(bytes: ByteArray): List<ArrayWrapper<A>> {
	  val byteThing = dtype.bytesThing(bytes)
	  return byteThing.parse2DArray()
	}
  }

  internal val weakActivations by lazyWeak {
	activations.await()
  }

  fun activationsFor(rLayer: InterTestLayer): ArrayWrapper<A> = weakActivations[rLayer.index]
  fun activationFor(neuron: InterTestNeuron) = dtype.rawActivation(weakActivations[neuron.layer.index][neuron.index])

  val data = object: CachedRAFProp<PixelData3>(pixelsRAF) {
	override fun decode(bytes: ByteArray): PixelData3 {
	  return readPixels(bytes)
	}
  }

  @Suppress("UNCHECKED_CAST")
  @PhaseOut private val weakTest = WeakReference(test) as WeakReference<LoadedOrFailedValueSlot<Test<A>>>

  val prediction by lazy {
	weakTest.get()!!.awaitRequireSuccessful().preds.await()[this]!!
  }

  val dtype get() = weakTest.get()!!.awaitRequireSuccessful().dtype


}


typealias PixelData2 = List<IntArray>
typealias PixelData3 = List<PixelData2>

fun ArrayReader.readPixels(): PixelData3 = readEachManually<ArrayReader, PixelData2> {
  readEachManually<ByteStringReader, IntArray> {
	val r = IntArray(count.toInt())
	for ((i, b) in read().raw.withIndex()) {
	  r[i] = b.toInt() and 0xff
	}
	r
  }
}

fun readPixels(cborPixelBytes3d: ByteArray): PixelData3 {
  cborPixelBytes3d.cborReader().readManually<ArrayReader, Unit> {
	return readPixels()
  }
}


fun ArrayReader.readFloatActivations() = readEachManually<ByteStringReader, FloatArrayWrapper> {
  val r = FloatArray(count.toInt()/FLOAT_BYTE_LEN)
  ByteBuffer.wrap(read().raw).asFloatBuffer().get(r)
  FloatArrayWrapper(r)
}

fun ArrayReader.readDoubleActivations() = readEachManually<ByteStringReader, DoubleArrayWrapper> {
  val r = DoubleArray(count.toInt()/DOUBLE_BYTE_LEN)
  ByteBuffer.wrap(read().raw).asDoubleBuffer().get(r)
  DoubleArrayWrapper(r)
}


sealed interface ImageActivationCborBytes<A: Number> {
  val bytes: ByteArray
  fun parse2DArray(): List<ArrayWrapper<A>>
  fun rawBytes() = bytes.cborReader().readManually<ArrayReader, ByteArray> {
	readEachManually<ByteStringReader, ByteArray> {
	  read().raw
	}.reduce { acc, bytes -> acc + bytes }
  }

  fun dtypeByteReadyBufferSequence(): Sequence<ByteBuffer>

}

@JvmInline value class ImageActivationCborBytesFloat32(override val bytes: ByteArray): ImageActivationCborBytes<Float> {

  override fun parse2DArray(): FloatActivationData {
	bytes.cborReader().readManually<ArrayReader, Unit> {
	  return readFloatActivations()
	}
  }


  override fun dtypeByteReadyBufferSequence(): Sequence<ByteBuffer> = sequence {
	bytes.cborReader().readManually<ArrayReader, Unit> {
	  readEachManually<ByteStringReader, Unit> {
		val buffer = ByteBuffer.wrap(read().raw)
		(FLOAT_BYTE_LEN until buffer.capacity() step FLOAT_BYTE_LEN).forEach {
		  buffer.limit(it)
		  yield(buffer)
		}
	  }
	}
  }


}

@JvmInline
value class ImageActivationCborBytesFloat64(override val bytes: ByteArray): ImageActivationCborBytes<Double> {
  override fun parse2DArray(): DoubleActivationData {
	bytes.cborReader().readManually<ArrayReader, Unit> {
	  return readDoubleActivations()
	}
  }

  override fun dtypeByteReadyBufferSequence(): Sequence<ByteBuffer> = sequence {
	bytes.cborReader().readManually<ArrayReader, Unit> {
	  readEachManually<ByteStringReader, Unit> {
		val buffer = ByteBuffer.wrap(read().raw)
		(DOUBLE_BYTE_LEN until buffer.capacity() step DOUBLE_BYTE_LEN).forEach {
		  buffer.limit(it)
		  yield(buffer)
		}
	  }
	}
  }

}