package matt.nn.deephys.load.cache

import matt.async.pool.MyThreadPriorities.DELETING_OLD_CACHE
import matt.file.DSStoreFile
import matt.file.construct.mFile
import matt.file.thismachine.thisMachine
import matt.lang.weak.lazyWeak
import matt.model.sys.Mac
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

	private val dSetCaches = DEEPHY_CACHE_DIR.listFiles()!!
	private val oldIDs = dSetCaches.map {
	  it.name.toIntOrNull()
	}

	private val newIDs = sequence {
	  var r = 0
	  while (true) {
		r++
		if (r !in oldIDs) yield(r)
	  }
	}.iterator()

	@Synchronized fun getNextID(): Int {
	  return newIDs.next()
	}

	init {
	  thread(isDaemon = true, priority = DELETING_OLD_CACHE.ordinal) {
		dSetCaches.forEach {
		  it.deleteIfExists()
		}
	  }
	}
  }

  private val id by lazy { getNextID() }
  private val folder by lazy { DEEPHY_CACHE_DIR.mkdir(id.toString()) }

  fun cache(im: DeephyImage, pixelBytes: ByteArray, read: (ByteArray)->PixelData3) {
	val f = folder["${im.index}.cbor"]
	f.writeBytes(pixelBytes)
	val dataCache by lazyWeak {
	  read(f.readBytes())
	}
	im.data.disposeValueAndSetCacheGetter {
	  dataCache
	}
  }


}