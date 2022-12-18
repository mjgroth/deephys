package matt.nn.deephys.load.test


import matt.async.pool.DaemonPool
import matt.async.thread.daemon
import matt.caching.compcache.globalman.RAMComputeCacheManager
import matt.cbor.err.CborParseException
import matt.cbor.read.major.array.ArrayReader
import matt.cbor.read.major.bytestr.ByteStringReader
import matt.cbor.read.major.map.MapReader
import matt.cbor.read.streamman.cborReader
import matt.cbor.read.withByteStoring
import matt.collect.list.awaitlist.BlockList
import matt.collect.list.awaitlist.BlockListBuilder
import matt.collect.queue.pollUntilEnd
import matt.file.CborFile
import matt.lang.List2D
import matt.lang.disabledCode
import matt.log.profile.mem.throttle
import matt.model.code.errreport.ThrowReport
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.model.obj.single.SingleCall
import matt.nn.deephys.load.async.AsyncLoader
import matt.nn.deephys.load.cache.CacheTool
import matt.nn.deephys.load.cache.DeephysCacheManager
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.mstate.ModelState
import matt.nn.deephys.model.importformat.neuron.TestNeuron
import matt.nn.deephys.model.importformat.testlike.TestOrLoader
import matt.obs.prop.BindableProperty
import matt.prim.float.FLOAT_BYTE_LEN
import matt.prim.str.elementsToString
import matt.prim.str.mybuild.string
import java.io.IOException
import java.nio.ByteBuffer
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
  private val pixelAndActCacher = DeephysCacheManager.newDatasetCacher()

  fun awaitNonUniformRandomImage() = finishedImages.await().random()

  fun awaitImage(index: Int) = finishedImages.await()[index]

  fun awaitFinishedTest(): Test = finishedTest.await()
  val numImages = LoadedValueSlot<ULong>()
  val progress by lazy { BindableProperty(0.0) }
  val cacheProgressPixels by lazy { BindableProperty(0.0) }
  val cacheProgressActs by lazy { BindableProperty(0.0) }
  private val numCachedPixels = AtomicInteger(0)
  private val numCachedActs = AtomicInteger(0)

  fun category(id: Int) = finishedImages
	.await()
	.asSequence()
	.map {
	  it.category
	}.first {
	  it.id == id
	}

  private var numDataBytes: Int? = null
  private var numActivationBytes: Int? = null

  private val numRead = AtomicInteger(0)

  //  val everythingBeforeBlock = ProfiledBlock("Everything-Before-${file.name}", uniqueSuffix = true)
  //  val everythingAfterBlock = ProfiledBlock("Everything-After-${file.name}", uniqueSuffix = true)
  //  private val cacheNeuronActsBlock = ProfiledBlock("Cache-matt.nn.deephys.model.importformat.neuron.Neuron-Activations-${file.name}", uniqueSuffix = true)


  val pixelsShapePerImage = LoadedValueSlot<String>()
  val activationsShapePerImage = LoadedValueSlot<String>()
  val infoString by lazy {
	string {
	  lineDelimited {
		+"Test:"
		+"\tname=${file.name}"
		+"\tnumImages=${numImages.await()}"
		+"\t${pixelsShapePerImage.await()}"
		+"\t${activationsShapePerImage.await()}"
	  }
	}
  }

  val start = SingleCall {    /*	matt.async.schedule.every(1.seconds) {
		  MemReport().println()
		  println(daemonPool.info())
		}*/
	daemon("TestLoader-${file.name}") {
	  if (!file.exists()) {
		signalFileNotFound()
		return@daemon
	  }
	  try {


		var localTestNeurons: Map<InterTestNeuron, TestNeuron>? = null
		var neuronActCacheTools: List<CacheTool>? = null
		//		val neuronActWriteBuff = ByteBuffer.allocate(FLOAT_BYTE_LEN)


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
			  println("numImages=$count")
			  val finishedImagesBuilder = BlockListBuilder<DeephyImage>(numImsInt)
			  finishedImages.putLoadedValue(finishedImagesBuilder.blockList)
			  localTestNeurons =
				model.neurons.associate {
				  it.interTest to TestNeuron(index = it.index, layerIndex = it.layer.index)
				}
			  println("numNeurons=${model.layers.map { it.neurons.size }.elementsToString()}")
			  neuronActCacheTools = localTestNeurons!!.values.map { neuron ->
				pixelAndActCacher.startCachingNeuronActs(neuron, numImsInt*FLOAT_BYTE_LEN) { bytes: ByteArray ->
				  FloatArray(numImsInt).also {
					ByteBuffer.wrap(bytes).asFloatBuffer().get(it)
				  }
				}
			  }


			  val ACTS_FOR_NEURONS_BUFF_SIZE = 1000
			  val activationByteMultiImBuffer = ArrayBlockingQueue<ImageActivationCborBytes>(ACTS_FOR_NEURONS_BUFF_SIZE)
			  readEachManually<MapReader, Unit> {

				//				val startInfo = everythingBeforeBlock.start()


				val imageID = nextValue<ULong>(requireKeyIs = "imageID").toInt()
				val categoryID = nextValue<ULong>(requireKeyIs = "categoryID").toInt()
				val category = nextValue<String>(requireKeyIs = "category")


				require(nextKeyOnly<String>() == "data")
				val imageData: ByteArray = if (numDataBytes == null) {
				  withByteStoring {
					val r = nextValueManualDontReadKey<ArrayReader, List2D<IntArray>> {
					  readPixels()
					}
					pixelsShapePerImage.putLoadedValue( "pixelsShapePerImage=[${r.size},${r[0].size},${r[0][0].size}]")
					r
				  }.let {
					numDataBytes = it.second.size
					it.second
				  }
				} else readNBytes(numDataBytes!!)

				var features: Map<String, String>? = null
				if (count.toInt() == 6) {
				  features = nextValue(requireKeyIs = "features")
				}

				val activationsBytes = ImageActivationCborBytes(
				  nextValueManual<MapReader, ByteArray>(
					requireKeyIs = "activations"
				  ) {
					require(nextKeyOnly<String>() == "activations")
					if (numActivationBytes == null) {
					  withByteStoring {
						val r = nextValueManualDontReadKey<ArrayReader, ActivationData> {
						  readActivations()
						}
						activationsShapePerImage.putLoadedValue("activationsLengthPerLayerPerImage=[${r.joinToString { it.size.toString() }}]")
						println(infoString)
						r
					  }.let {
						numActivationBytes = it.second.size
						it.second
					  }
					} else readNBytes(numActivationBytes!!)
				  }
				)






				if (numRead.incrementAndGet()%1000 == 0) {
				  throttle("test loader")
				}


				val state = ModelState()

				//				everythingBeforeBlock.stop(startInfo)

				//				cacheNeuronActsBlock.with {
				//				val toolItr = cacheNeuronActsBlock.subBlock("get itr") {

				//				}
				//				val seq = cacheNeuronActsBlock.subBlock("get seq") {
				/*val seq = activationsBytes.floatByteReadyBufferSequence()*/
				//				}

				//				cacheNeuronActsBlock.subBlock("write all") {
				activationByteMultiImBuffer.put(activationsBytes)
				if (activationByteMultiImBuffer.size == ACTS_FOR_NEURONS_BUFF_SIZE || nextImageIndex == lastImageIndex) {


				  val toolItr = neuronActCacheTools!!.iterator()

				  /*	  val itrs = activationByteMultiImBuffer
						  .pollUntilEnd()
						  .asSequence()
						  .map {
							it.floatByteReadyBufferSequence().iterator()
						  }.toList()*/

				  val imageActBytes = activationByteMultiImBuffer
					.pollUntilEnd()
					.map {
					  it.floatBytes()
					}

				  val siz = imageActBytes.size
				  val buff = ByteArray(FLOAT_BYTE_LEN*siz)

				  //				  val fir = itrs.first()
				  (0 until imageActBytes[0].size step FLOAT_BYTE_LEN).forEach { n ->
					val tool = toolItr.next()
					imageActBytes.forEachIndexed { idx, it ->
					  System.arraycopy(it, n, buff, idx*FLOAT_BYTE_LEN, FLOAT_BYTE_LEN)
					  /*it.next().get(buff, idx*FLOAT_BYTE_LEN, FLOAT_BYTE_LEN)*/
					}
					tool.outputStream.write(buff)
				  }
				  /*  while (fir.hasNext()) {

					  //					  val tool = cacheNeuronActsBlock.subBlock("get next tool") {
					  val tool = toolItr.next()
					  //					  }

					  //					  cacheNeuronActsBlock.subBlock("fill buffer") {
					  itrs.forEachIndexed { idx, it ->
						it.next().get(buff, idx*FLOAT_BYTE_LEN, FLOAT_BYTE_LEN)
					  }
					  //					  }

					  //					  cacheNeuronActsBlock.subBlock("write floats") {
					  tool.outputStream.write(buff)
					  //					  }


					}*/


				  /*seq.forEach { fourByteReadyBuf ->*/

				  //					val deed = cacheNeuronActsBlock.subBlock("get deed") {

				  //					}


				  /*deed.write(
					fourByteReadyBuf,
					destOffset = nextImageIndex*FLOAT_BYTE_LEN
				  )*/


				  /* val n = cacheNeuronActsBlock.subBlock("get next tool") {
					 toolItr.next()
				   }*/
				  /*
										cacheNeuronActsBlock.subBlock("fill buffer") {
										  fourByteReadyBuf.get(buff)
										}

										cacheNeuronActsBlock.subBlock("write float") {
										  n.outputStream.write(buff)
										}*/


				  //					cacheNeuronActsBlock.subBlock("clear") {
				  //					  neuronActWriteBuff.clear()
				  //					}

				  /*cacheNeuronActsBlock.subBlock("subBlock") {
					subBlock("subBlockExample") {
					  1 + 1
					}
				  }*/

				  /*}*/


				  //				}

				}
				//				}


				//				val startInfoAfter = everythingAfterBlock.start()


				//				val subCreateImage = everythingAfterBlock.subBlock("create image")
				//				val subCreateImageStartInfo = subCreateImage.start()
				val deephyImage = DeephyImage(
				  imageID = imageID,
				  categoryID = categoryID,
				  category = category,
				  index = nextImageIndex++,
				  testLoader = this@TestLoader,
				  model = this@TestLoader.model,
				  test = finishedTest,
				  activations = state,
				  features = features
				).apply {

				  //				  subCreateImage.stop(subCreateImageStartInfo)


				  val im = this

				  //				  everythingAfterBlock.subBlock("schedule executions") {


				  state.apply {
					daemonPool.execute {
					  disabledCode {
						activations.putGetter {
						  activationsBytes.parse2DFloatArray()
						}
					  }
					}








					daemonPool.executeLowPriority {
					  pixelAndActCacher.cacheImActs(im, activationsBytes.bytes) {
						ImageActivationCborBytes(it).parse2DFloatArray()
					  }
					  val n = numCachedActs.incrementAndGet()
					  cacheProgressActs.value = (n.toDouble())/numImsDouble
					}
				  }

				  daemonPool.execute {
					disabledCode {
					  data.putGetter { readPixels(imageData) }
					}
				  }
				  daemonPool.executeLowPriority {
					pixelAndActCacher.cachePixels(this@apply, imageData, ::readPixels)
					val n = numCachedPixels.incrementAndGet()                    /*if (n%1000 == 0) println("n=$n")*/
					cacheProgressPixels.value = (n.toDouble())/numImsDouble
				  }
				  //				  }


				}

				//				everythingAfterBlock.subBlock("process finished im") {

				finishedImagesBuilder += deephyImage
				if (nextImageIndex%100 == 0) {
				  //				  if (numberOfIms > 10000u) {
				  //					println("finishedImages=${nextImageIndex}")
				  //					everythingBeforeBlock.report()
				  //					cacheNeuronActsBlock.report()
				  //					everythingAfterBlock.report()
				  //					RAFCache.instances.forEach {
				  //					  it.profSeek.report()
				  //					  it.profWriteBytes.report()
				  //					}
				  //					println("progress=${progress.value}")
				  //				  }
				  progress.value = nextImageIndex/numberOfIms.toDouble()
				}
			  }


			  //				everythingAfterBlock.stop(startInfoAfter)

			  //			  }
			}


			//			cacheNeuronActsBlock.subBlock("flush") {
			//			neuronActCacheTools!!.forEach {
			//			  it.outputStream.flush()
			//			}
			//			}


			neuronActCacheTools!!.forEach {
			  it.outputStream.flush()
			  /*it.deed.close()*/
			  it.cacheOp()
			}
			pixelAndActCacher.closeWritingOnNeuronActs()

			progress.value = 1.0
			//			everythingBeforeBlock.report()
			//			cacheNeuronActsBlock.report()
			//			everythingAfterBlock.report()
			//			RAFCache.instances.forEach {
			//			  it.profSeek.report()
			//			it.profWriteBytes.report()
			//			}

			finishedTest.putLoadedValue(Test(
			  name = name,
			  suffix = suffix,
			  images = finishedImages.await(),
			  model = this@TestLoader.model
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


  val cacheMan by lazy {
	RAMComputeCacheManager()
  }

}

typealias ActivationData = List<FloatArray>
typealias PixelData2 = List<IntArray>
typealias PixelData3 = List<PixelData2>

private fun ArrayReader.readPixels(): PixelData3 = readEachManually<ArrayReader, PixelData2> {
  readEachManually<ByteStringReader, IntArray> {
	val r = IntArray(count.toInt())
	for ((i, b) in read().raw.withIndex()) {
	  r[i] = b.toInt() and 0xff
	}
	r
  }
}

private fun readPixels(cborPixelBytes3d: ByteArray): PixelData3 {
  cborPixelBytes3d.cborReader().readManually<ArrayReader, Unit> {
	return readPixels()
  }
}

private fun ArrayReader.readActivations() = readEachManually<ByteStringReader, FloatArray> {
  val r = FloatArray(count.toInt()/FLOAT_BYTE_LEN)
  ByteBuffer.wrap(read().raw).asFloatBuffer().get(r)
  r
}

@JvmInline
value class ImageActivationCborBytes(val bytes: ByteArray) {

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