package matt.nn.deephys.load.test.imageloader

import matt.async.pool.DaemonPool
import matt.cbor.read.major.array.ArrayReader
import matt.cbor.read.major.map.MapReader
import matt.cbor.read.withByteStoring
import matt.collect.list.awaitlist.BlockList
import matt.collect.list.awaitlist.BlockListBuilder
import matt.collect.queue.pollUntilEnd
import matt.lang.List2D
import matt.lang.disabledCode
import matt.lang.l
import matt.log.profile.mem.throttle
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.nn.deephys.load.cache.Cacher
import matt.nn.deephys.load.cache.DeephysCacheManager
import matt.nn.deephys.load.cache.raf.EvenlySizedRAFCache
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.dtype.FloatActivationData
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.Test
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.im.ImageActivationCborBytes
import matt.nn.deephys.model.importformat.im.readFloatActivations
import matt.nn.deephys.model.importformat.im.readPixels
import matt.nn.deephys.model.importformat.neuron.TestNeuron
import matt.obs.prop.BindableProperty
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger


class ImageSetLoader(private val testLoader: TestLoader) {

  companion object {
	private val daemonPool = DaemonPool()
  }

  val finishedImages = LoadedValueSlot<BlockList<DeephyImage<*>>>()
  var localTestNeurons: Map<InterTestNeuron, TestNeuron<*>>? = null
  var neuronActCacheTools: List<Cacher>? = null
  val datasetHDCache = DeephysCacheManager.newDatasetCache()


  private var numDataBytes: Int? = null
  private var numActivationBytes: Int? = null
  private val numRead = AtomicInteger(0)
  val pixelsShapePerImage = LoadedValueSlot<List<Int>>()
  val activationsShapePerImage = LoadedValueSlot<List<Int>>()
  private val numCachedPixels = AtomicInteger(0)
  private val numCachedActs = AtomicInteger(0)
  val progress by lazy { BindableProperty(0.0) }
  val cacheProgressPixels by lazy { BindableProperty(0.0) }
  val cacheProgressActs by lazy { BindableProperty(0.0) }

  private var didRead = false

  fun readImages(
	reader: MapReader,
	dtype: DType<*>,
	finishedTest: LoadedValueSlot<Test<*>>
  ) = reader.apply {

	require(!didRead)
	didRead = true

	nextValueManualDontReadKey<ArrayReader, Unit>() {
	  val numberOfIms = count
	  val numImsDouble = numberOfIms.toDouble()
	  val numImsInt = numberOfIms.toInt()
	  val lastImageIndex = numImsInt - 1
	  var nextImageIndex = 0
	  testLoader.numImages.putLoadedValue(count)
	  val finishedImagesBuilder = BlockListBuilder<DeephyImage<*>>(numImsInt)
	  finishedImages.putLoadedValue(finishedImagesBuilder.blockList)

	  val actsCacheSize = numImsInt*dtype.byteLen
	  val evenRAF = EvenlySizedRAFCache(datasetHDCache.neuronsRAF, actsCacheSize)

	  localTestNeurons = testLoader.model.neurons.associate {
		it.interTest to TestNeuron(
		  index = it.index,
		  layerIndex = it.layer.index,
		  activationsRAF = evenRAF,
		  numIms = numImsInt,
		  dType = dtype
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



		nextKeyOrValueOnly(requireIs = "data")
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
		  nextKeyOrValueOnly(requireIs = "activations")
		  if (numActivationBytes == null) {
			withByteStoring {
			  val r = nextValueManualDontReadKey<ArrayReader, FloatActivationData> {
				readFloatActivations()
			  }
			  activationsShapePerImage.putLoadedValue(r.map { it.size })
			  println(testLoader.infoString)
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
		  testLoader = testLoader,
		  model = testLoader.model,
		  test = finishedTest,
		  features = features,
		  activationsRAF = activationsRAF!!,
		  pixelsRAF = pixelsRAF!!,
		  dtype = dtype
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


  }
}