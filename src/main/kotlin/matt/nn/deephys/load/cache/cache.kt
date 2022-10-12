package matt.nn.deephys.load.cache

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import matt.async.thread.daemon
import matt.file.DSStoreFile
import matt.file.construct.mFile
import matt.file.thismachine.thisMachine
import matt.lang.weak.lazyWeak
import matt.model.sys.Mac
import matt.nn.deephys.model.importformat.DeephyImage

class PixelCacher {

  companion object {
	init {
	  require(thisMachine is Mac)
	}

	private val DEEPHY_USER_DATA_DIR = mFile(
	  System.getProperty("user.home")
	)["Library"]["Application Support"].mkdir("Deephys")
	private val DEEPHY_CACHE_DIR = DEEPHY_USER_DATA_DIR.mkdir("Cache")

	private val prepThread = daemon {
	  DEEPHY_USER_DATA_DIR.listFiles()!!.forEach {
		require(it is DSStoreFile || it == DEEPHY_CACHE_DIR) {
		  "unknown file $it found in the user data directory"
		}
	  }
	  DEEPHY_CACHE_DIR.listFiles()!!.forEach {
		it.deleteIfExists()
	  }
	}

	private var nextID = 1

	@Synchronized
	fun getNextID() = nextID++

  }

  private val id by lazy { getNextID() }
  private val folder by lazy { DEEPHY_CACHE_DIR.mkdir(id.toString()) }

  fun cache(im: DeephyImage) {
	prepThread.join()
	val f = folder["${im.index}.cbor"]
	f.writeBytes(Cbor.encodeToByteArray(im.data.await()))
	val dataCache by lazyWeak<List<List<IntArray>>> {
	  Cbor.decodeFromByteArray(f.readBytes())
	}
	im.data.disposeValueAndSetCacheGetter {
	  dataCache
	}
  }


}