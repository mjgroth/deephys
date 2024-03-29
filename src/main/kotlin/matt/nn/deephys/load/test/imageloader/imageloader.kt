package matt.nn.deephys.load.test.imageloader

import matt.async.thread.pool.DaemonPoolExecutor
import matt.cbor.read.major.array.ArrayReader
import matt.cbor.read.major.map.MapReader
import matt.cbor.read.withByteStoring
import matt.collect.list.awaitlist.BlockList
import matt.collect.list.awaitlist.BlockListBuilder
import matt.collect.queue.j.JQueueWrapper
import matt.collect.queue.pollUntilEnd
import matt.lang.assertions.require.requireNot
import matt.lang.atomic.AtomicInt
import matt.lang.common.List2D
import matt.lang.common.disabledCode
import matt.lang.common.l
import matt.log.profile.mem.throttle
import matt.nn.deephys.load.async.AsyncLoader.DirectLoadedOrFailedValueSlot
import matt.nn.deephys.load.cache.Cacher
import matt.nn.deephys.load.cache.DeephysCacheManager
import matt.nn.deephys.load.cache.raf.EvenlySizedRAFCache
import matt.nn.deephys.load.test.LoadException
import matt.nn.deephys.load.test.PostDtypeTestLoader
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
import matt.prim.str.elementsToString
import java.util.concurrent.ArrayBlockingQueue


class ImageSetLoader<A: Number>(
    private val testLoader: TestLoader,
    private val postDtypeTestLoader: PostDtypeTestLoader<A>
) {

    companion object {
        private val daemonPool = DaemonPoolExecutor()
    }

    val finishedImages = postDtypeTestLoader.createSlot<BlockList<DeephyImage<A>>>()
    var localTestNeurons: Map<InterTestNeuron, TestNeuron<A>>? = null
    var neuronActCacheTools: List<Cacher>? = null
    val datasetHDCache = DeephysCacheManager.newDatasetCache()


    private var numDataBytes: Int? = null
    private var numActivationBytes: Int? = null
    private val numRead = AtomicInt(0)
    val pixelsShapePerImage = postDtypeTestLoader.createSlot<List<Int>>()
    val activationsShapePerImage = postDtypeTestLoader.createSlot<List<Int>>()
    private val numCachedPixels = AtomicInt(0)
    private val numCachedActs = AtomicInt(0)


    private var didRead = false

    fun readImages(
        reader: MapReader,
        dtype: DType<A>,
        finishedTest: DirectLoadedOrFailedValueSlot<Test<A>>
    ) = reader.apply {

        requireNot(didRead)
        didRead = true

        nextValueManualDontReadKey<ArrayReader, Unit> {
            val numberOfIms = count
            val numImsDouble = numberOfIms.toDouble()
            val numImsInt = numberOfIms.toInt()
            val lastImageIndex = numImsInt - 1
            var nextImageIndex = 0
            testLoader.numImages.putLoadedValue(count)
            val finishedImagesBuilder = BlockListBuilder<DeephyImage<A>>(numImsInt)
            finishedImages.putLoadedValue(finishedImagesBuilder.blockList)

            val actsCacheSize = numImsInt * dtype.byteLen
            val evenRAF = EvenlySizedRAFCache(datasetHDCache.neuronsRAF, actsCacheSize)

            localTestNeurons =
                testLoader.model.neurons.associate {
                    it.interTest to
                        TestNeuron(
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
            val activationByteMultiImBuffer =
                ArrayBlockingQueue<ImageActivationCborBytes<*>>(
                    ACTS_FOR_NEURONS_BUFF_SIZE
                )
            readEachManually<MapReader, Unit> {
                val imageID = nextValue<ULong>(requireKeyIs = "imageID").toInt()
                val categoryID = nextValue<ULong>(requireKeyIs = "categoryID").toInt()
                val category = nextValue<String>(requireKeyIs = "category")



                nextKeyOrValueOnly(requireIs = "data")
                val imageData: ByteArray =
                    if (numDataBytes == null) {
                        withByteStoring {
                            val r =
                                nextValueManualDontReadKey<ArrayReader, List2D<IntArray>> {
                                    readPixels()
                                }
                            val imDims = l(r.size, r[0].size, r[0][0].size)
                            pixelsShapePerImage.putLoadedValue(imDims)
                            if (r.size != 3) {
                                throw LoadException("Images should have 3 color channels. The first dimension should be a length of 3, but the encountered dimensions were ${imDims.elementsToString()} ")
                            }
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


                val bytes =
                    nextValueManual<MapReader, ByteArray>(
                        requireKeyIs = "activations"
                    ) {
                        nextKeyOrValueOnly(requireIs = "activations")
                        if (numActivationBytes == null) {
                            withByteStoring {
                                val r =
                                    nextValueManualDontReadKey<ArrayReader, FloatActivationData> {
                                        readFloatActivations()
                                    }
                                val actsShapePerIm = r.map { it.size }
                                activationsShapePerImage.putLoadedValue(actsShapePerIm)

                                val modelShape = testLoader.model.layers.map { it.neurons.size }

                                if (
                                    actsShapePerIm.size != testLoader.model.layers.size
                                    || modelShape.zip(actsShapePerIm).any { it.first != it.second }
                                ) {
                                    throw LoadException("Activations shape from .test file (${actsShapePerIm.elementsToString()}) does not match shape from .model file (${modelShape.elementsToString()})")
                                }





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






                if (numRead.incrementAndGet() % 1000 == 0) {
                    throttle("test loader")
                }



                activationByteMultiImBuffer.put(activationsBytes)
                if (activationByteMultiImBuffer.size == ACTS_FOR_NEURONS_BUFF_SIZE || nextImageIndex == lastImageIndex) {


                    val toolItr = neuronActCacheTools!!.iterator()

                    val imageActBytes =
                        JQueueWrapper(activationByteMultiImBuffer).pollUntilEnd().map {
                            it.rawBytes()
                        }

                    val siz = imageActBytes.size
                    val buff = ByteArray(dtype.byteLen * siz)

                    (0..<imageActBytes[0].size step dtype.byteLen).forEach { n ->
                        val tool = toolItr.next()
                        imageActBytes.forEachIndexed { idx, it ->
                            System.arraycopy(it, n, buff, idx * dtype.byteLen, dtype.byteLen)
                        }
                        tool.write(buff)
                    }
                }

                val deephyImage =
                    DeephyImage(
                        imageID = imageID,
                        categoryID = categoryID,
                        category = category,
                        index = nextImageIndex++,
                        testLoader = postDtypeTestLoader,
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
                            this@ImageSetLoader.testLoader.progress.cacheProgressActs.value = (n.toDouble()) / numImsDouble
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
                            this@ImageSetLoader.testLoader.progress.cacheProgressPixels.value = (n.toDouble()) / numImsDouble
                            if (n == numImsInt) datasetHDCache.pixelsRAF.closeWriting()
                        }
                    }


                finishedImagesBuilder += deephyImage
                if (nextImageIndex % 100 == 0) {

                    testLoader.progress.progress.value = nextImageIndex / numberOfIms.toDouble()
                }
            }
        }
    }
}
