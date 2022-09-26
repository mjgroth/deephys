package matt.nn.deephy.load.test

import matt.async.thread.daemon
import matt.cbor.CborItemReader
import matt.cbor.read.major.array.ArrayReader
import matt.cbor.read.major.map.MapReader
import matt.cbor.read.streamman.cborReader
import matt.cbor.read.streamman.readCbor
import matt.file.CborFile
import matt.lang.sync
import matt.log.profile.tic
import matt.log.warn
import matt.model.errreport.ThrowReport
import matt.model.latch.asyncloaded.AsyncLoadingValue
import matt.model.obj.single.SingleCall
import matt.nn.deephy.load.async.AsyncLoader
import matt.nn.deephy.model.DeephyImage
import matt.nn.deephy.model.Model
import matt.nn.deephy.model.ModelState
import matt.nn.deephy.model.Test
import java.io.IOException
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


private val dataLoaderDispatcher: ExecutorService = Executors.newCachedThreadPool {
  Thread(it).apply {
	isDaemon = true
  }
}.apply {
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
private var loadJobStartedCount = 0
private var loadJobFinishedCount = 0

class TestLoader(
  file: CborFile, val model: Model
): AsyncLoader(file) {

  private var finishedTest: Test? = null
  private val finishedImages = mutableListOf<DeephyImage>()

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
	finishedLoadingLatch.await()
	return finishedTest!!
  }

  val numImages = AsyncLoadingValue<ULong>()

  fun category(id: Int): String {
	var i = 0
	do {
	  finishedImages.sync {
		while (i < finishedImages.size) {
		  if (finishedImages[i].categoryID == id) {
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
	  require(file.exists())
	  val t = tic("TestLoader Daemon")
	  try {
		val stream = file.inputStream()
		t.toc("got stream")
		try {
		  val reader = stream.cborReader()
		  reader.readManually<MapReader> {
			if (count != 3.toULong()) {
			  warn("expected 3 name-value pairs but got $count")
			  signalParseError()
			  return@daemon
			}
			val name = nextValue<String>(requireKeyIs = "name")
			val suffix = nextValue<String>(requireKeyIs = "suffix")

			nextValueManual<ArrayReader>(requireKeyIs = "images") {
			  numImages.putLoadedValue(count)
			  println("numImages=$count")

			  readEachManually<MapReader> {
				val imageID = nextValue<String>(requireKeyIs = "imageID")
				val categoryID = nextValue<String>(requireKeyIs = "categoryID")
				val category = nextValue<String>(requireKeyIs = "category")
				val imageData: Any = if (numDataBytes == null) {
				  var willBeNumHeaderBytes = 0

				  nextValueManual<ArrayReader>(requireKeyIs = "data") {
					willBeNumHeaderBytes += head.numBytes
					require(hasCount) /*make sure there is a cunt*/

					val r = dataReader.mapElements {
					  willBeNumHeaderBytes += head.numBytes
					  require(it.length != null)
					  it.mapElements {
						require(length != null)
						it as ByteArray
						willBeNumHeaderBytes += numCborBytesFor(it)
						val r = IntArray(it.size)
						for ((i, b) in it.withIndex()) {
						  r[i] = b.toInt() and 0xff
						}
						r
					  }
					}
					numDataBytes = willBeNumHeaderBytes
					r
				  }


				} else {
				  val key = reader.next()
				  require(key == "data")
				  reader.stream.readNBytes(numDataBytes!!)
				}
				val modelReader = it.nextValue(requireKeyIs = "activations") as CborMapReader
				val activationsThing: Any = if (numActivationBytes == null) {
				  var willBeNumHeaderBytes = 0
				  val activationsReader = modelReader.nextValue(requireKeyIs = "activations") as CborArrayReader
				  willBeNumHeaderBytes += activationsReader.numHeaderBytes
				  val activations = activationsReader.mapElements {
					it as ByteArray
					willBeNumHeaderBytes += numCborBytesFor(it)
					val r = FloatArray(it.size)
					ByteBuffer.wrap(it).asFloatBuffer().get(r)
					r
				  }
				  numActivationBytes = willBeNumHeaderBytes
				  println("numActivationBytes=$numActivationBytes")
				  activations
				} else {
				  val key = reader.next()
				  require(key == "activations")
				  reader.stream.readNBytes(numActivationBytes!!)
				}

				@Suppress("KotlinConstantConditions", "UNCHECKED_CAST")
				val deephyImage = DeephyImage(
				  imageID = imageID as Int, categoryID = categoryID as Int, category = category as String,
				  activations = ModelState().apply {
					when (activationsThing) {
					  is List<*> -> activations.putLoadedValue(activationsThing as List<FloatArray>)
					  else       -> {
						dataLoaderDispatcher.execute {
						  loadJobStartedCount += 1
						  val workerReader = (activationsThing as ByteArray).inputStream().buffered()
						  val workerCborReader = CborItemReader(workerReader)
						  val actsReader = workerCborReader.next() as CborArrayReader
						  require(actsReader.length != null)
						  val theData = actsReader.mapElements {
							it as ByteArray
							val r = FloatArray(it.size)
							ByteBuffer.wrap(it).asFloatBuffer().get(r)
							r
						  }
						  activations.putLoadedValue(theData)
						  loadJobFinishedCount += 1
						}
					  }
					}
				  }
				).apply {
				  model.putLoadedValue(this@TestLoader.model)
				  index.putLoadedValue(finishedImages.size)
				  when (imageData) {
					is List<*> -> data.putLoadedValue(imageData as List<List<IntArray>>)
					else       -> {
					  dataLoaderDispatcher.execute {
						loadJobStartedCount += 1
						val workerReader = (imageData as ByteArray).inputStream().buffered()
						val workerCborReader = CborItemReader(workerReader)
						val dataReader = workerCborReader.next() as CborArrayReader
						require(dataReader.length != null)
						val theData = dataReader.mapElements {
						  it as CborArrayReader
						  require(it.length != null)
						  it.mapElements {
							it as ByteArray
							val r = IntArray(it.size)
							for ((i, b) in it.withIndex()) {
							  r[i] = b.toInt() and 0xff
							}
							r
						  }
						}
						data.putLoadedValue(theData)
						loadJobFinishedCount += 1
					  }
					  imageData as ByteArray
					}
				  }
				}
				finishedImages.sync {
				  finishedImages += deephyImage
				  if (finishedImages.size%2500 == 0) {
					println("finished loading ${finishedImages.size} images")
				  }
				}
			  }
			}
			finishedTest = Test(
			  name = name as String, suffix = suffix as String?, images = finishedImages
			).apply {
			  model = this@TestLoader.model
			}
			finishedImages.forEach {
			  it.test.putLoadedValue(finishedTest!!)
			}
			signalFinishedLoading()
		  }
		} catch (e: CborParseException) {
		  signalParseError()
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