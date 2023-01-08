package matt.nn.deephys.load.test


import matt.async.pool.DaemonPool
import matt.async.thread.daemon
import matt.cbor.err.CborParseException
import matt.cbor.read.major.array.ArrayReader
import matt.cbor.read.major.map.MapReader
import matt.cbor.read.streamman.cborReader
import matt.cbor.read.withByteStoring
import matt.collect.list.awaitlist.BlockList
import matt.collect.list.awaitlist.BlockListBuilder
import matt.collect.queue.pollUntilEnd
import matt.file.CborFile
import matt.lang.List2D
import matt.lang.disabledCode
import matt.lang.l
import matt.log.profile.mem.throttle
import matt.model.code.errreport.ThrowReport
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.model.obj.single.SingleCall
import matt.nn.deephys.load.async.AsyncLoader
import matt.nn.deephys.load.cache.Cacher
import matt.nn.deephys.load.cache.DeephysCacheManager
import matt.nn.deephys.load.cache.raf.EvenlySizedRAFCache
import matt.nn.deephys.load.test.testcache.TestRAMCache
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test
import matt.nn.deephys.model.importformat.im.ActivationData
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.im.ImageActivationCborBytes
import matt.nn.deephys.model.importformat.im.readActivations
import matt.nn.deephys.model.importformat.im.readPixels
import matt.nn.deephys.model.importformat.neuron.TestNeuron
import matt.nn.deephys.model.importformat.testlike.TestOrLoader
import matt.obs.prop.BindableProperty
import matt.prim.float.FLOAT_BYTE_LEN
import matt.prim.str.elementsToString
import matt.prim.str.mybuild.string
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger


class TestLoader(
  file: CborFile, val model: Model
): AsyncLoader(file), TestOrLoader {

  companion object {
	private val daemonPool = DaemonPool()
  }

  override val test get() = awaitFinishedTest()
  private var finishedTest = LoadedValueSlot<Test>()
  private val finishedImages = LoadedValueSlot<BlockList<DeephyImage>>()
  override val finishedLoadingAwaitable = finishedTest
  private val datasetHDCache = DeephysCacheManager.newDatasetCache()

  fun awaitNonUniformRandomImage() = finishedImages.await().random()

  fun awaitImage(index: Int) = finishedImages.await()[index]

  fun awaitFinishedTest(): Test = finishedTest.await()
  val numImages = LoadedValueSlot<ULong>()
  val progress by lazy { BindableProperty(0.0) }
  val cacheProgressPixels by lazy { BindableProperty(0.0) }
  val cacheProgressActs by lazy { BindableProperty(0.0) }
  private val numCachedPixels = AtomicInteger(0)
  private val numCachedActs = AtomicInteger(0)

  fun category(id: Int) = finishedImages.await().asSequence().map {
	it.category
  }.first {
	it.id == id
  }

  private var numDataBytes: Int? = null
  private var numActivationBytes: Int? = null

  private val numRead = AtomicInteger(0)

  private val pixelsShapePerImage = LoadedValueSlot<List<Int>>()
  private val activationsShapePerImage = LoadedValueSlot<List<Int>>()

  private val infoString by lazy {
	string {
	  lineDelimited {
		+"Test:"
		+"\tname=${file.name}"
		+"\tnumImages=${numImages.await()}"
		+"\tpixelsShapePerImage=${pixelsShapePerImage.await().elementsToString()}"
		+"\tactivationsShapePerImage=${activationsShapePerImage.await().elementsToString()}"
	  }
	}
  }

  @OptIn(ExperimentalStdlibApi::class) val start = SingleCall {
	daemon("TestLoader-${file.name}") {
	  if (!file.exists()) {
		signalFileNotFound()
		return@daemon
	  }
	  try {
		var localTestNeurons: Map<InterTestNeuron, TestNeuron>? = null
		var neuronActCacheTools: List<Cacher>? = null
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
			  val numImsInt = numberOfIms.toInt()
			  val lastImageIndex = numImsInt - 1
			  var nextImageIndex = 0
			  numImages.putLoadedValue(count)
			  val finishedImagesBuilder = BlockListBuilder<DeephyImage>(numImsInt)
			  finishedImages.putLoadedValue(finishedImagesBuilder.blockList)

			  val actsCacheSize = numImsInt*FLOAT_BYTE_LEN
			  val evenRAF = EvenlySizedRAFCache(datasetHDCache.neuronsRAF, actsCacheSize)

			  localTestNeurons = model.neurons.associate {
				it.interTest to TestNeuron(
				  index = it.index, layerIndex = it.layer.index, activationsRAF = evenRAF, numIms = numImsInt
				)
			  }

			  neuronActCacheTools = localTestNeurons!!.values.map { it.activations.cacher }

			  var activationsRAF: EvenlySizedRAFCache? = null
			  var pixelsRAF: EvenlySizedRAFCache? = null


			  val ACTS_FOR_NEURONS_BUFF_SIZE = 1000
			  val activationByteMultiImBuffer = ArrayBlockingQueue<ImageActivationCborBytes>(ACTS_FOR_NEURONS_BUFF_SIZE)
			  readEachManually<MapReader, Unit> {
				val imageID = nextValue<ULong>(requireKeyIs = "imageID").toInt()
				val categoryID = nextValue<ULong>(requireKeyIs = "categoryID").toInt()
				val category = nextValue<String>(requireKeyIs = "category")


				require(nextKeyOnly<String>() == "data")
				val imageData: ByteArray = if (numDataBytes == null) {
				  withByteStoring {
					val r = nextValueManualDontReadKey<ArrayReader, List2D<IntArray>> {
					  readPixels()
					}
					pixelsShapePerImage.putLoadedValue(l(r.size, r[0].size, r[0][0].size))
					r
				  }.let {
					numDataBytes = it.second.size
					pixelsRAF = EvenlySizedRAFCache(datasetHDCache.pixelsRAF, numDataBytes!!)
					it.second
				  }
				} else readNBytes(numDataBytes!!)

				var features: Map<String, String>? = null
				if (count.toInt() == 6) {
				  features = nextValue(requireKeyIs = "features")
				}

				val activationsBytes = ImageActivationCborBytes(nextValueManual<MapReader, ByteArray>(
				  requireKeyIs = "activations"
				) {
				  require(nextKeyOnly<String>() == "activations")
				  if (numActivationBytes == null) {
					withByteStoring {
					  val r = nextValueManualDontReadKey<ArrayReader, ActivationData> {
						readActivations()
					  }
					  activationsShapePerImage.putLoadedValue(r.map { it.size })
					  println(infoString)
					  r
					}.let {
					  numActivationBytes = it.second.size
					  activationsRAF = EvenlySizedRAFCache(datasetHDCache.activationsRAF, numActivationBytes!!)
					  it.second
					}
				  } else readNBytes(numActivationBytes!!)
				})






				if (numRead.incrementAndGet()%1000 == 0) {
				  throttle("test loader")
				}



				activationByteMultiImBuffer.put(activationsBytes)
				if (activationByteMultiImBuffer.size == ACTS_FOR_NEURONS_BUFF_SIZE || nextImageIndex == lastImageIndex) {


				  val toolItr = neuronActCacheTools!!.iterator()

				  val imageActBytes = activationByteMultiImBuffer.pollUntilEnd().map {
					it.floatBytes()
				  }

				  val siz = imageActBytes.size
				  val buff = ByteArray(FLOAT_BYTE_LEN*siz)

				  (0..<imageActBytes[0].size step FLOAT_BYTE_LEN).forEach { n ->
					val tool = toolItr.next()
					imageActBytes.forEachIndexed { idx, it ->
					  System.arraycopy(it, n, buff, idx*FLOAT_BYTE_LEN, FLOAT_BYTE_LEN)
					}
					tool.write(buff)
				  }


				}
				val deephyImage = DeephyImage(
				  imageID = imageID,
				  categoryID = categoryID,
				  category = category,
				  index = nextImageIndex++,
				  testLoader = this@TestLoader,
				  model = this@TestLoader.model,
				  test = finishedTest,
				  features = features,
				  activationsRAF = activationsRAF!!,
				  pixelsRAF = pixelsRAF!!
				).apply {


				  daemonPool.execute {
					disabledCode {
					  activations.strong {
						activationsBytes.parse2DFloatArray()
					  }
					}
				  }
				  daemonPool.executeLowPriority {
					activations.cache(activationsBytes.bytes)
					val n = numCachedActs.incrementAndGet()
					cacheProgressActs.value = (n.toDouble())/numImsDouble
					if (n == numImsInt) datasetHDCache.activationsRAF.closeWriting()
				  }

				  disabledCode {
					daemonPool.execute {
					  data.strong {
						readPixels(imageData)
					  }
					}
				  }
				  daemonPool.executeLowPriority {
					data.cache(imageData)
					val n = numCachedPixels.incrementAndGet()
					cacheProgressPixels.value = (n.toDouble())/numImsDouble
					if (n == numImsInt) datasetHDCache.pixelsRAF.closeWriting()
				  }


				}


				finishedImagesBuilder += deephyImage
				if (nextImageIndex%100 == 0) {

				  progress.value = nextImageIndex/numberOfIms.toDouble()
				}
			  }


			}



			neuronActCacheTools!!.forEach {
			  it.finalize()
			}
			datasetHDCache.neuronsRAF.closeWriting()

			progress.value = 1.0


			finishedTest.putLoadedValue(Test(
			  name = name,
			  suffix = suffix,
			  images = finishedImages.await(),
			  model = this@TestLoader.model,
			  testRAMCache = testRAMCache
			).apply {
			  testNeurons = localTestNeurons
			  preds.startLoading()
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


  override val testRAMCache by lazy { TestRAMCache() }

}


