package matt.nn.deephy.load.test

import matt.async.thread.daemon
import matt.cbor.CborArrayReader
import matt.cbor.CborMapReader
import matt.cbor.CborParseException
import matt.cbor.CborStreamReader
import matt.cbor.numCborBytesFor
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
import java.util.concurrent.Executors


private val dataLoaderDispatcher = Executors.newCachedThreadPool {
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

@Suppress("OPT_IN_USAGE") class TestLoader(
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

  val numImages = AsyncLoadingValue<Int>()

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
		  val reader = CborStreamReader(stream)

		  reader.streamMap {
			if (length != 3) {
			  warn("expected 3 name-value pairs but got $length")
			  signalParseError()
			  return@daemon
			}
			val name = nextValue(requireKeyIs = "name")
			val suffix = nextValue(requireKeyIs = "suffix")

			(nextValue(requireKeyIs = "images") as CborArrayReader).apply {
			  numImages.putLoadedValue(length!!)
			  println("numImages=$length")
			  forEachElement {

				//				val imageLoadingStopwatch = tic("loading image")
				//				imageLoadingStopwatch.toc("1")

				//								println("im?:$it")


				it as CborMapReader

				val imageID = it.nextValue(requireKeyIs = "imageID")
				//				imageLoadingStopwatch.toc("2")
				val categoryID = it.nextValue(requireKeyIs = "categoryID")
				//				imageLoadingStopwatch.toc("3")
				val category = it.nextValue(requireKeyIs = "category")
				//				imageLoadingStopwatch.toc("4")


				val imageData: Any = if (numDataBytes == null) {


				  var willBeNumHeaderBytes = 0

				  val dataReader = it.nextValue(requireKeyIs = "data") as CborArrayReader

				  willBeNumHeaderBytes += dataReader.numHeaderBytes

				  //				  imageLoadingStopwatch.toc("5")
				  require(dataReader.length != null)

				  val r = dataReader.mapElements {
					it as CborArrayReader


					willBeNumHeaderBytes += it.numHeaderBytes

					require(it.length != null)




					it.mapElements {

					  require(length != null)

					  it as ByteArray


					  //					  val r = IntArray(it.size)


					  //					  ByteBuffer.wrap(it).asIntBuffer().get(r)

					  //					  it as CborArrayReader
					  //					  require(it.length != null)


					  willBeNumHeaderBytes += numCborBytesFor(it)


					  //					  willBeNumHeaderBytes += it.numBytesIfListOfBytesIncludingHeader()
					  //					  it.readSetSizeByteArrayAsInt8s()


					  //					  return numHeaderBytes + length

					  //					  it.map { it.toUInt() }

					  val r = IntArray(it.size)
					  for ((i, b) in it.withIndex()) {
						//						r[i] = b.toUInt()
						r[i] = b.toInt() and 0xff
					  }
					  r

					}


				  }

				  numDataBytes = willBeNumHeaderBytes
				  r
				} else {
				  val key = reader.nextDataItem()
				  require(key == "data")
				  reader.stream.readNBytes(numDataBytes!!)
				}


				//				imageLoadingStopwatch.toc("6")

				val modelReader = it.nextValue(requireKeyIs = "activations") as CborMapReader

				val activationsThing: Any = if (numActivationBytes == null) {

				  //				  imageLoadingStopwatch.toc("7")

				  var willBeNumHeaderBytes = 0

				  val activationsReader = modelReader.nextValue(requireKeyIs = "activations") as CborArrayReader

				  willBeNumHeaderBytes += activationsReader.numHeaderBytes

				  //				  imageLoadingStopwatch.toc("8")

				  println("activationsReader.numHeaderBytes=${activationsReader.numHeaderBytes}")
				  println("activationsReader.length=${activationsReader.length}")

				  val activations = activationsReader.mapElements {

					it as CborArrayReader

					willBeNumHeaderBytes += it.numBytesIfListOfFloat32sIncludingHeader()


					it.readSetSizeFloat32Array()


				  }

				  numActivationBytes = willBeNumHeaderBytes

				  println("numActivationBytes=$numActivationBytes")
				  activations
				} else {
				  val key = reader.nextDataItem()
				  require(key == "activations")
				  reader.stream.readNBytes(numActivationBytes!!)
				  //				  println("skipped!")
				}


				//				imageLoadingStopwatch.toc("9")

				@Suppress("KotlinConstantConditions", "UNCHECKED_CAST")
				val deephyImage = DeephyImage(
				  imageID = imageID as Int, categoryID = categoryID as Int, category = category as String,
				  activations = ModelState().apply {
					when (activationsThing) {
					  is List<*> -> activations.putLoadedValue(activationsThing as List<List<Float>>)
					  else       -> {
						dataLoaderDispatcher.execute {
						  loadJobStartedCount += 1
						  val workerReader = (activationsThing as ByteArray).inputStream().buffered()
						  val workerCborReader = CborStreamReader(workerReader)
						  val actsReader = workerCborReader.nextDataItem() as CborArrayReader
						  require(actsReader.length != null)
						  val theData = actsReader.mapElements {
							it as CborArrayReader
							require(it.length != null)
							it.readSetSizeFloat32Array()
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
						val workerCborReader = CborStreamReader(workerReader)
						val dataReader = workerCborReader.nextDataItem() as CborArrayReader
						require(dataReader.length != null)
						val theData = dataReader.mapElements {
						  it as CborArrayReader
						  require(it.length != null)
						  it.mapElements {
							it as ByteArray
							//							it as CborArrayReader
							//							require(it.length != null)

							//							it.readSetSizeByteArrayAsInt8s()

							//							val r = IntArray(it.size)


							//							ByteBuffer.wrap(it).asIntBuffer().get(r)
							//							r
							val r = IntArray(it.size)
							for ((i, b) in it.withIndex()) {
							  r[i] = b.toInt() and 0xff
							  //							  b shr 8
							  //							  r[i] = /b.toUInt()
							  //							  require(r[i] >= 0.toUInt())
							  //							  require(r[i] <= 255.toUInt()) {
							  //								"r[i]=${r[i]}, int = ${b.toInt()}, byte = ${b}"
							  //							  }
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

				//				imageLoadingStopwatch.toc("10")

				finishedImages.sync {
				  finishedImages += deephyImage
				  if (finishedImages.size%2500 == 0) {
					println("finished loading ${finishedImages.size} images")
				  }
				}

				//				imageLoadingStopwatch.toc("11")

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