package matt.nn.deephys.load.test

import matt.async.thread.daemon
import matt.cbor.err.CborParseException
import matt.cbor.read.major.array.ArrayReader
import matt.cbor.read.major.bytestr.ByteStringReader
import matt.cbor.read.major.map.MapReader
import matt.cbor.read.streamman.cborReader
import matt.file.CborFile
import matt.lang.List2D
import matt.lang.disabledCode
import matt.lang.sync
import matt.log.profile.stopwatch.tic
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
import matt.obs.prop.BindableProperty
import java.io.IOException
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger


private val dataLoaderDispatcher: ExecutorService = Executors.newCachedThreadPool {
  Thread(it).apply {
	isDaemon = true
  }
}.apply {
  disabledCode {
	daemon {
	  var lastReported = ""
	  while (true) {
		val report = "$loadJobFinishedCount/$loadJobStartedCount jobs finished"
		if (report != lastReported) {
		  println(report)
		  lastReported = report
		}
		sleep(1000)
	  }
	}
  }
}

private val loadJobStartedCount = AtomicInteger()
private val loadJobFinishedCount = AtomicInteger()

interface TestOrLoader {
  val test: Test
}

class TestLoader(
  file: CborFile, val model: Model
): AsyncLoader(file), TestOrLoader {

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

  fun awaitFinishedTest(): Test {
	/*finishedLoadingLatch.await()*/
	return finishedTest.await()
  }

  val numImages = LoadedValueSlot<ULong>()

  val progress by lazy {
	BindableProperty(0.0)
  }

  val cacheProgress by lazy {
	BindableProperty(0.0)
  }

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
	val t = tic("TestLoader", enabled = false)
	t.toc(1)
	daemon {
	  t.toc(2)
	  if (!file.exists()) {
		signalFileNotFound()
		return@daemon
	  }
	  t.toc(3)
	  //	  val t = tic("TestLoader Daemon")
	  try {
		t.toc(4)
		val stream = file.inputStream()
		t.toc(5)
		//		t.toc("got stream")
		try {
		  val reader = stream.cborReader()
		  t.toc(6)
		  reader.readManually<MapReader, Unit> {
			t.toc(7)
			//			t.toc("reading cbor manually")

			expectCount(3UL)
			t.toc(8)
			val name = nextValue<String>(requireKeyIs = "name")
			val suffix = nextValue<String?>(requireKeyIs = "suffix")
			t.toc(9)
			//			t.toc("got name and suffix")

			nextValueManual<ArrayReader, Unit>(requireKeyIs = "images") {
			  val numIms = count
			  numImages.putLoadedValue(count)
			  println("numImages=$count")

			  readEachManually<MapReader, Unit> {

				//				val t = tic("reading image")

				//				t.toc("reading image")

				val imageID = nextValue<ULong>(requireKeyIs = "imageID").toInt()
				val categoryID = nextValue<ULong>(requireKeyIs = "categoryID").toInt()
				val category = nextValue<String>(requireKeyIs = "category")

				//				t.toc("got first 3 props")

				val imageData: Any = if (numDataBytes == null) {
				  var willBeNumHeaderBytes = 0
				  nextValueManual<ArrayReader, List2D<IntArray>>(requireKeyIs = "data") {
					willBeNumHeaderBytes += head.numBytes
					val r = readEachManually<ArrayReader, List<IntArray>> {
					  willBeNumHeaderBytes += head.numBytes
					  readEachManually<ByteStringReader, IntArray> {
						willBeNumHeaderBytes += head.numBytes + count.toInt()
						val r = IntArray(count.toInt())
						for ((i, b) in read().raw.withIndex()) {
						  r[i] = b.toInt() and 0xff
						}
						r
					  }
					}
					numDataBytes = willBeNumHeaderBytes
					r
				  }
				} else {
				  require(nextKeyOnly<String>() == "data")
				  readNBytes(numDataBytes!!)
				}

				//				t.toc("got imageData")

				val activationsThing: Any = nextValueManual<MapReader, Any>(
				  requireKeyIs = "activations"
				) {
				  if (numActivationBytes == null) {
					var willBeNumHeaderBytes = 0
					nextValueManual<ArrayReader, List<FloatArray>>(requireKeyIs = "activations") {
					  willBeNumHeaderBytes += head.numBytes
					  val activations = readEachManually<ByteStringReader, FloatArray> {
						willBeNumHeaderBytes += head.numBytes + count.toInt()
						val r = FloatArray(count.toInt()/4)
						ByteBuffer.wrap(read().raw.toByteArray()).asFloatBuffer().get(r)
						r
					  }
					  numActivationBytes = willBeNumHeaderBytes
					  //					  println("activations.size=${activations.size}")
					  //					  println("activations[0].size=${activations[0].size}")
					  activations
					}
				  } else {
					require(nextKeyOnly<String>() == "activations")
					readNBytes(numActivationBytes!!)
				  }
				}

				//				t.toc("got activations")


				@Suppress("UNCHECKED_CAST")
				val deephyImage = DeephyImage(
				  imageID = imageID,
				  categoryID = categoryID,
				  category = category,
				  index = finishedImages.size,
				  testLoader = this@TestLoader,
				  model = this@TestLoader.model,
				  test = finishedTest,
				  activations = ModelState().apply {
					when (activationsThing) {
					  is List<*> -> activations.putLoadedValue(activationsThing as List<FloatArray>)
					  else       -> {
						dataLoaderDispatcher.execute {
						  loadJobStartedCount.incrementAndGet()
						  val workerReader = (activationsThing as ByteArray).inputStream().buffered()
						  val workerCborReader = workerReader.cborReader()
						  workerCborReader.readManually<ArrayReader, Unit> {
							require(hasCount)
							val theData = readEachManually<ByteStringReader, FloatArray> {
							  val r = FloatArray(count.toInt()/4)
							  ByteBuffer.wrap(read().raw.toByteArray()).asFloatBuffer().get(r)
							  r
							}
							activations.putLoadedValue(theData)
							loadJobFinishedCount.incrementAndGet()
						  }
						}
					  }
					}
				  }
				).apply {
				  when (imageData) {
					is List<*> -> data.putLoadedValue(imageData as List<List<IntArray>>)
					else       -> {
					  dataLoaderDispatcher.execute {
						loadJobStartedCount.incrementAndGet()
						val workerReader = (imageData as ByteArray).inputStream().buffered()
						workerReader.cborReader().readManually<ArrayReader, Unit> {
						  require(hasCount)
						  val theData = readEachManually<ArrayReader, List<IntArray>> {
							require(hasCount)
							readEachManually<ByteStringReader, IntArray> {
							  val r = IntArray(count.toInt())
							  for ((i, b) in read().raw.withIndex()) {
								r[i] = b.toInt() and 0xff
							  }
							  r
							}
						  }
						  data.putLoadedValue(theData)
						  loadJobFinishedCount.incrementAndGet()
						}

					  }
					  imageData as ByteArray
					}
				  }
				}

				//				t.toc("got image")

				finishedImages.sync {
				  finishedImages += deephyImage

				  if (finishedImages.size%100 == 0) {
					progress.value = finishedImages.size/numIms.toDouble()
				  }
				}

				//				t.toc("put image\n\n")

			  }
			}



			t.toc(10)
			finishedTest.putLoadedValue(Test(
			  name = name, suffix = suffix, images = finishedImages
			).apply {
			  model = this@TestLoader.model
			  daemon {
				startPreloadingActs()
			  }
			})
			t.toc(11)
			/*finishedImages.forEach {
			  it.test.putLoadedValue(finishedTest!!)
			}*/
			t.toc(12)
			signalFinishedLoading()
			t.toc(13)


			stream.close()

			val siz = finishedImages.size
			finishedImages.forEachIndexed { index, im ->
			  pixelCacher.cache(im)
			  cacheProgress.value = (index + 1.0)/siz
			}

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
	t.toc(14)
  }
}