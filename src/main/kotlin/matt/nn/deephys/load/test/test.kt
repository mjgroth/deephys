package matt.nn.deephys.load.test


import matt.async.pool.DaemonPool
import matt.async.thread.daemon
import matt.cbor.err.CborParseException
import matt.cbor.read.major.array.ArrayReader
import matt.cbor.read.major.map.MapReader
import matt.cbor.read.major.txtstr.TextStringReader
import matt.cbor.read.streamman.cborReader
import matt.cbor.read.withByteStoring
import matt.collect.list.awaitlist.BlockList
import matt.collect.list.awaitlist.BlockListBuilder
import matt.collect.queue.pollUntilEnd
import matt.file.CborFile
import matt.lang.List2D
import matt.lang.disabledCode
import matt.lang.err
import matt.lang.l
import matt.log.profile.mem.throttle
import matt.model.code.errreport.ThrowReport
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.model.obj.single.SingleCall
import matt.nn.deephys.load.async.AsyncLoader
import matt.nn.deephys.load.cache.Cacher
import matt.nn.deephys.load.cache.DeephysCacheManager
import matt.nn.deephys.load.cache.raf.EvenlySizedRAFCache
import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.dtype.Float32
import matt.nn.deephys.load.test.dtype.Float64
import matt.nn.deephys.load.test.dtype.FloatActivationData
import matt.nn.deephys.load.test.testcache.TestRAMCache
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.im.ImageActivationCborBytes
import matt.nn.deephys.model.importformat.im.readFloatActivations
import matt.nn.deephys.model.importformat.im.readPixels
import matt.nn.deephys.model.importformat.neuron.TestNeuron
import matt.nn.deephys.model.importformat.testlike.TestOrLoader
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.obs.prop.BindableProperty
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
  override val dtype get() = test.dtype
  private var finishedTest = LoadedValueSlot<Test<*>>()
  private val finishedImages = LoadedValueSlot<BlockList<DeephyImage<*>>>()
  override val finishedLoadingAwaitable = finishedTest
  private val datasetHDCache = DeephysCacheManager.newDatasetCache()
  fun awaitNonUniformRandomImage() = finishedImages.await().random()
  fun awaitImage(index: Int) = finishedImages.await()[index]
  fun awaitFinishedTest(): Test<*> = finishedTest.await()
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

  val start = SingleCall {
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

			val nextKey = nextKeyOnly<String>()


			val dtype = if (nextKey == "dtype") {
			  nextValueManualDontReadKey<TextStringReader, DType<*>> {
				val str = this.read().raw
				when (str) {
				  "float32" -> Float32
				  "float64" -> Float64
				  else      -> err("str == $str")
				}
			  }
			} else if (nextKey == "images") {
			  Float32
			} else {
			  err("nextKey == $nextKey")
			}

			nextValueManualDontReadKey<ArrayReader, Unit>() {
			  val numberOfIms = count
			  val numImsDouble = numberOfIms.toDouble()
			  val numImsInt = numberOfIms.toInt()
			  val lastImageIndex = numImsInt - 1
			  var nextImageIndex = 0
			  numImages.putLoadedValue(count)
			  val finishedImagesBuilder = BlockListBuilder<DeephyImage<*>>(numImsInt)
			  finishedImages.putLoadedValue(finishedImagesBuilder.blockList)

			  val actsCacheSize = numImsInt*dtype.byteLen
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
			  val activationByteMultiImBuffer = ArrayBlockingQueue<ImageActivationCborBytes<*>>(
				ACTS_FOR_NEURONS_BUFF_SIZE
			  )
			  readEachManually<MapReader, Unit> {
				val imageID = nextValue<ULong>(requireKeyIs = "imageID").toInt()
				val categoryID = nextValue<ULong>(requireKeyIs = "categoryID").toInt()
				val category = nextValue<String>(requireKeyIs = "category")



				nextKeyOnly(requireIs = "data")
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


				val bytes = nextValueManual<MapReader, ByteArray>(
				  requireKeyIs = "activations"
				) {
				  nextKeyOnly(requireIs = "activations")
				  if (numActivationBytes == null) {
					withByteStoring {
					  val r = nextValueManualDontReadKey<ArrayReader, FloatActivationData> {
						readFloatActivations()
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
				}

				val activationsBytes = dtype.bytesThing(bytes)






				if (numRead.incrementAndGet()%1000 == 0) {
				  throttle("test loader")
				}



				activationByteMultiImBuffer.put(activationsBytes)
				if (activationByteMultiImBuffer.size == ACTS_FOR_NEURONS_BUFF_SIZE || nextImageIndex == lastImageIndex) {


				  val toolItr = neuronActCacheTools!!.iterator()

				  val imageActBytes = activationByteMultiImBuffer.pollUntilEnd().map {
					it.rawBytes()
				  }

				  val siz = imageActBytes.size
				  val buff = ByteArray(dtype.byteLen*siz)

				  (0..<imageActBytes[0].size step dtype.byteLen).forEach { n ->
					val tool = toolItr.next()
					imageActBytes.forEachIndexed { idx, it ->
					  System.arraycopy(it, n, buff, idx*dtype.byteLen, dtype.byteLen)
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
				  pixelsRAF = pixelsRAF!!,
//				  dtype = dtype
				).apply {


				/*  daemonPool.execute {
					disabledCode {
					  activations.strong {
						activationsBytes.parse2DArray()
					  }
					}
				  }*/
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
			  testRAMCache = testRAMCache,
			  dtype = dtype
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

class PreppedTestLoader<N: Number>(val tl: TestLoader, override val dtype: DType<N>): TestOrLoader {
  override val test: Test<N> get() = tl.test
  override val testRAMCache: TestRAMCache get() = tl.testRAMCache
}


