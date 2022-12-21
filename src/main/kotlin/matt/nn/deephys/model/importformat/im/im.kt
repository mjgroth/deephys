package matt.nn.deephys.model.importformat.im

import matt.cbor.read.major.array.ArrayReader
import matt.cbor.read.major.bytestr.ByteStringReader
import matt.cbor.read.streamman.cborReader
import matt.fx.graphics.wrapper.style.FXColor
import matt.lang.anno.PhaseOut
import matt.lang.weak.lazyWeak
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.load.cache.RAFCaches
import matt.nn.deephys.load.cache.raf.EvenlySizedRAFCache
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test
import matt.prim.float.FLOAT_BYTE_LEN
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

class DeephyImage(
  val imageID: Int,
  categoryID: Int,
  category: String,
  val testLoader: TestLoader,
  val index: Int,
  val model: Model,
  val features: Map<String, String>?,
  test: LoadedValueSlot<Test>,
  activationsRAF: EvenlySizedRAFCache,
  pixelsRAF: EvenlySizedRAFCache,
): RAFCaches() {

  val category = Category(id = categoryID, label = category)

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


  val activations = object: CachedRAFProp<ActivationData>(activationsRAF) {
	override fun decode(bytes: ByteArray): ActivationData {
	  return ImageActivationCborBytes(bytes).parse2DFloatArray()
	}
  }

  internal val weakActivations by lazyWeak {
	activations.await()
  }

  fun activationsFor(rLayer: InterTestLayer): FloatArray = weakActivations[rLayer.index]
  fun activationFor(neuron: InterTestNeuron) = RawActivation(weakActivations[neuron.layer.index][neuron.index])

  val data = object: CachedRAFProp<PixelData3>(pixelsRAF) {
	override fun decode(bytes: ByteArray): PixelData3 {
	  return readPixels(bytes)
	}
  }

  @PhaseOut private val weakTest = WeakReference(test)

  val prediction by lazy {
	weakTest.get()!!.await().preds.await()[this]!!
  }


}

typealias ActivationData = List<FloatArray>
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



fun ArrayReader.readActivations() = readEachManually<ByteStringReader, FloatArray> {
  val r = FloatArray(count.toInt()/FLOAT_BYTE_LEN)
  ByteBuffer.wrap(read().raw).asFloatBuffer().get(r)
  r
}

@JvmInline value class ImageActivationCborBytes(val bytes: ByteArray) {

  fun parse2DFloatArray(): ActivationData {
	bytes.cborReader().readManually<ArrayReader, Unit> {
	  return readActivations()
	}
  }

  fun floatByteReadyBufferSequence() = sequence {
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

  fun floatBytes() = bytes.cborReader().readManually<ArrayReader, ByteArray> {
	readEachManually<ByteStringReader, ByteArray> {
	  read().raw
	}.reduce { acc, bytes -> acc + bytes }
  }


}