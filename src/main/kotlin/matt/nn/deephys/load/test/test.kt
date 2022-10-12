package matt.nn.deephys.load.test

import matt.async.pool.DaemonPool
import matt.async.thread.daemon
import matt.cbor.err.CborParseException
import matt.cbor.read.major.array.ArrayReader
import matt.cbor.read.major.bytestr.ByteStringReader
import matt.cbor.read.major.map.MapReader
import matt.cbor.read.streamman.cborReader
import matt.cbor.read.withByteStoring
import matt.file.CborFile
import matt.lang.List2D
import matt.lang.sync
import matt.model.errreport.ThrowReport
import matt.model.latch.asyncloaded.LoadedValueSlot
import matt.model.obj.single.SingleCall
import matt.nn.deephys.load.async.AsyncLoader
import matt.nn.deephys.load.cache.PixelCacher
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.importformat.DeephyImage
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.ModelState
import matt.nn.deephys.model.importformat.Test
import matt.nn.deephys.model.importformat.TestOrLoader
import matt.obs.prop.BindableProperty
import java.io.IOException
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger


class TestLoader(
  file: CborFile, val model: Model
): AsyncLoader(file), TestOrLoader {

  companion object {
	private val daemonPool = DaemonPool()
  }

  override val test get() = awaitFinishedTest()
  private var finishedTest = LoadedValueSlot<Test>()
  private val finishedImages = mutableListOf<DeephyImage>()
  override val finishedLoadingAwaitable = finishedTest
  private val pixelCacher = PixelCacher()

  fun awaitNonUniformRandomImage(): DeephyImage {
	while (true) {
	  finishedImages.sync {
		if (finishedImages.size > 0) {
		  return finishedImages.random()
		}
	  }
	  sleep(10)
	}
  }

  fun awaitImage(index: Int): DeephyImage {
	while (true) {
	  finishedImages.sync {
		if (finishedImages.size > index) {
		  return finishedImages[index]
		}
	  }
	  sleep(10)
	}
  }

  fun awaitFinishedTest(): Test = finishedTest.await()
  val numImages = LoadedValueSlot<ULong>()
  val progress by lazy { BindableProperty(0.0) }
  val cacheProgress by lazy { BindableProperty(0.0) }
  val numCached = AtomicInteger(0)

  fun category(id: Int): Category {
	var i = 0
	do {
	  finishedImages.sync {
		while (i < finishedImages.size) {
		  if (finishedImages[i].category.id == id) {
			return finishedImages[i].category
		  }
		  i++
		}
	  }
	  sleep(10)
	} while (true)
  }

  private var numDataBytes: Int? = null
  private var numActivationBytes: Int? = null


  val start = SingleCall {
	daemon {
	  if (!file.exists()) {
		signalFileNotFound()
		return@daemon
	  }
	  try {
		val stream = file.inputStream()
		try {
		  val reader = stream.cborReader()
		  reader.readManually<MapReader, Unit> {


			expectCount(3UL)
			val name = nextValue<String>(requireKeyIs = "name")
			val suffix = nextValue<String?>(requireKeyIs = "suffix")

			nextValueManual<ArrayReader, Unit>(requireKeyIs = "images") {
			  val numberOfIms = count
			  val numImsDouble = numberOfIms.toDouble()
			  numImages.putLoadedValue(count)
			  println("numImages=$count")

			  readEachManually<MapReader, Unit> {


				val imageID = nextValue<ULong>(requireKeyIs = "imageID").toInt()
				val categoryID = nextValue<ULong>(requireKeyIs = "categoryID").toInt()
				val category = nextValue<String>(requireKeyIs = "category")


				require(nextKeyOnly<String>() == "data")
				val imageData: ByteArray = if (numDataBytes == null) {
				  withByteStoring {
					nextValueManualDontReadKey<ArrayReader, List2D<IntArray>> {
					  readPixels()
					}
				  }.let {
					numDataBytes = it.second.size
					it.second
				  }
				} else readNBytes(numDataBytes!!)

				val activationsBytes: ByteArray = nextValueManual<MapReader, ByteArray>(
				  requireKeyIs = "activations"
				) {
				  require(nextKeyOnly<String>() == "activations")
				  if (numActivationBytes == null) {
					withByteStoring {
					  nextValueManualDontReadKey<ArrayReader, List<FloatArray>> {
						readActivations()
					  }
					}.let {
					  numActivationBytes = it.second.size
					  it.second
					}
				  } else readNBytes(numActivationBytes!!)
				}


				val deephyImage = DeephyImage(imageID = imageID, categoryID = categoryID, category = category,
				  index = finishedImages.size, testLoader = this@TestLoader, model = this@TestLoader.model,
				  test = finishedTest, activations = ModelState().apply {
					daemonPool.execute {
					  val workerReader = activationsBytes.inputStream().buffered()
					  val workerCborReader = workerReader.cborReader()
					  workerCborReader.readManually<ArrayReader, Unit> {
						activations.putLoadedValue(readActivations())
					  }
					}
				  }).apply {
				  daemonPool.execute {
					data.putLoadedValue(readPixels(imageData))
					daemonPool.executeLowPriority {
					  pixelCacher.cache(this@apply, imageData, ::readPixels)
					  val n = numCached.incrementAndGet()
					  cacheProgress.value = (n.toDouble())/numImsDouble
					}
				  }
				}

				finishedImages.sync {
				  finishedImages += deephyImage
				  if (finishedImages.size%100 == 0) {
					progress.value = finishedImages.size/numberOfIms.toDouble()
				  }
				}
			  }
			}
			finishedImages.sync {
			  progress.value = 1.0
			}

			finishedTest.putLoadedValue(Test(
			  name = name, suffix = suffix, images = finishedImages
			).apply {
			  model = this@TestLoader.model
			  daemon {
				startPreloadingActs()
			  }
			})
			signalFinishedLoading()
		  }
		} catch (e: CborParseException) {
		  signalParseError(e)
		  return@daemon
		}
		stream.close()

	  } catch (e: IOException) {
		ThrowReport(Thread.currentThread(), e).print()
		signalStreamNotOk()
	  }
	}
  }
}

typealias PixelData2 = List<IntArray>
typealias PixelData3 = List<PixelData2>

private fun ArrayReader.readActivations() = readEachManually<ByteStringReader, FloatArray> {
  val r = FloatArray(count.toInt()/4)
  ByteBuffer.wrap(read().raw.toByteArray()).asFloatBuffer().get(r)
  r
}

private fun ArrayReader.readPixels() = readEachManually<ArrayReader, PixelData2> {
  readEachManually<ByteStringReader, IntArray> {
	val r = IntArray(count.toInt())
	for ((i, b) in read().raw.withIndex()) {
	  r[i] = b.toInt() and 0xff
	}
	r
  }
}

private fun readPixels(byteArray: ByteArray): PixelData3 {
  val workerReader = byteArray.inputStream().buffered()
  workerReader.cborReader().readManually<ArrayReader, Unit> {
	return readPixels()
  }
}