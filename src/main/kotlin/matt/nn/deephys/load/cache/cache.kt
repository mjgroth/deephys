package matt.nn.deephys.load.cache

import matt.async.pool.MyThreadPriorities.DELETING_OLD_CACHE
import matt.file.DSStoreFile
import matt.file.MFile
import matt.file.commons.APP_SUPPORT_FOLDER
import matt.lang.function.Op
import matt.nn.deephys.load.cache.raf.RAFCache
import matt.nn.deephys.load.test.ActivationData
import matt.nn.deephys.load.test.PixelData3
import matt.nn.deephys.model.importformat.DeephyImage
import matt.nn.deephys.model.importformat.TestNeuron
import java.io.OutputStream
import kotlin.concurrent.thread


object DeephysCacheManager {
  private val DEEPHY_USER_DATA_DIR = APP_SUPPORT_FOLDER.mkdir("Deephys")
  private val DEEPHY_CACHE_DIR = DEEPHY_USER_DATA_DIR.mkdir("Cache")

  init {
	DEEPHY_USER_DATA_DIR.listFiles()!!.forEach {
	  require(it is DSStoreFile || it == DEEPHY_CACHE_DIR) {
		"unknown file $it found in the user data directory"
	  }
	}
  }


  private val DATA_SETS_CACHE_DIR = DEEPHY_CACHE_DIR.mkdir("datasets")

  private val weirdCaches1 = DEEPHY_CACHE_DIR.listFiles()!!.filter { it !in listOf(DATA_SETS_CACHE_DIR) }

  private val oldDatasetCaches = DATA_SETS_CACHE_DIR.listFiles()!!

  init {
	thread(name = "delete caches", isDaemon = true, priority = DELETING_OLD_CACHE.ordinal) {
	  /*KEEP THESE PRINT STATEMENTS UNTIL I CAN CONFIRM THIS IS WORKING*/
	  println("started deleting weird caches")
	  weirdCaches1.forEach {
		it.deleteIfExists()
	  }
	  println("finished deleting weird caches")
	  println("started deleting old dataset caches")
	  oldDatasetCaches.forEach {
		it.deleteIfExists()
	  }
	  println("finished deleting old dataset caches")
	}
  }

  private val oldDatasetIDs = oldDatasetCaches.mapNotNull {
	it.name.toIntOrNull()
  }

  private val newIDs = (1..Int.MAX_VALUE)
	.asSequence()
	.filter { it !in oldDatasetIDs }
	.iterator()

  @Synchronized private fun getNextDatasetID(): Int {
	return newIDs.next()
  }

  fun newDatasetCacher(): DatasetCacher {
	return DatasetCacherImpl(DATA_SETS_CACHE_DIR.mkdir(getNextDatasetID().toString()))
  }


  private class DatasetCacherImpl(
	folder: MFile,
  ): DatasetCacher {

	companion object {
	  const val PIXELS_CBOR = "pixels.cbor"
	  const val ACTS_CBOR = "activations.cbor"
	  const val ACTS_FLOAT = "activations.float"
	}

	//	override val recentList = EvitctingQueue<MFile>(10)

	val imagesFolder = folder.mkdir("images")
	val neuronsRAF = RAFCache(folder["neurons.raf"])

	override fun cachePixels(im: DeephyImage, pixelBytes: ByteArray, read: (ByteArray)->PixelData3) {
	  val imFold = imagesFolder.mkdir("${im.index}")
	  val f = imFold[PIXELS_CBOR]
	  f.writeBytes(pixelBytes)
	  im.data.putLazyWeakGetter {
		/*did not save f on purpose in order to reduce RAM usage*/
		read(imagesFolder["${im.index}"][PIXELS_CBOR].readBytes())
	  }
	}

	override fun cacheImActs(im: DeephyImage, actsBytes: ByteArray, read: (ByteArray)->ActivationData) {
	  val imFold = imagesFolder.mkdir("${im.index}")
	  val f = imFold[ACTS_CBOR]
	  f.writeBytes(actsBytes)
	  im.activations.activations.putLazyWeakGetter {
		/*did not save f on purpose in order to reduce RAM usage*/
		read(imagesFolder["${im.index}"][ACTS_CBOR].readBytes())
	  }
	}

	override fun startCachingNeuronActs(neuron: TestNeuron, size: Int, read: (ByteArray)->FloatArray): CacheTool {
	  val deed = neuronsRAF.rent(size)
	  return CacheTool(deed.outputStream().buffered(2000)) {
		neuron.activations.putLazyWeakGetter {
		  read(deed.read())
		}
	  }
	}


	override fun closeWritingOnNueronActs() {
	  neuronsRAF.closeWriting()
	}
  }


}

class CacheTool(
  /*val outputChannel: FileChannel,*/
  val outputStream: OutputStream,
  val cacheOp: Op
)

interface DatasetCacher {
  fun cachePixels(im: DeephyImage, pixelBytes: ByteArray, read: (ByteArray)->PixelData3)
  fun cacheImActs(im: DeephyImage, actsBytes: ByteArray, read: (ByteArray)->ActivationData)
  fun startCachingNeuronActs(neuron: TestNeuron, size: Int, read: (ByteArray)->FloatArray): CacheTool
  fun closeWritingOnNueronActs()
  //  val recentList: Queue<MFile>
}


