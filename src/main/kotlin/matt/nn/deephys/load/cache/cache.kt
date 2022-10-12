package matt.nn.deephys.load.cache

import matt.async.pool.MyThreadPriorities.DELETING_OLD_CACHE
import matt.file.DSStoreFile
import matt.file.construct.mFile
import matt.file.thismachine.thisMachine
import matt.lang.weak.lazyWeak
import matt.model.sys.Mac
import matt.nn.deephys.load.test.ActivationData
import matt.nn.deephys.load.test.PixelData3
import matt.nn.deephys.model.importformat.DeephyImage
import kotlin.concurrent.thread

class PixelCacher {

  companion object {
	init {
	  require(thisMachine is Mac)
	}

	private val DEEPHY_USER_DATA_DIR = mFile(
	  System.getProperty("user.home")
	)["Library"]["Application Support"].mkdir("Deephys")
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
	  thread(isDaemon = true, priority = DELETING_OLD_CACHE.ordinal) {
		weirdCaches1.forEach {
		  it.deleteIfExists()
		}
		oldDatasetCaches.forEach {
		  it.deleteIfExists()
		}
	  }
	}

	private val oldDatasetIDs = oldDatasetCaches.mapNotNull {
	  it.name.toIntOrNull()
	}

	private val newIDs = sequence {
	  var r = 0
	  while (true) {
		r++
		if (r !in oldDatasetIDs) yield(r)
	  }
	}.iterator()

	@Synchronized fun getNextDatasetID(): Int {
	  return newIDs.next()
	}


  }

  private val id by lazy { getNextDatasetID() }
  private val folder by lazy { DATA_SETS_CACHE_DIR.mkdir(id.toString()) }

  fun cachePixels(im: DeephyImage, pixelBytes: ByteArray, read: (ByteArray)->PixelData3) {
	val imFold = folder.mkdir("${im.index}")
	val f = imFold["pixels.cbor"]
	f.writeBytes(pixelBytes)
	val dataCache by lazyWeak {
	  /*did not save f on purpose in order to reduce RAM usage*/
	  read(folder["${im.index}"]["pixels.cbor"].readBytes())
	}
	im.data.putGetter {
	  dataCache
	}
  }

  fun cacheActs(im: DeephyImage, actsBytes: ByteArray, read: (ByteArray)->ActivationData) {
	val imFold = folder.mkdir("${im.index}")
	val f = imFold["activations.cbor"]
	f.writeBytes(actsBytes)
	val dataCache by lazyWeak {
	  /*did not save f on purpose in order to reduce RAM usage*/
	  read(folder["${im.index}"]["activations.cbor"].readBytes())
	}
	im.activations.activations.putGetter {
	  dataCache
	}
  }


}