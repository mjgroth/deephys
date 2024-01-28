package matt.nn.deephys.load.cache

import matt.lang.model.file.FsFile
import matt.file.ext.mkFold
import matt.file.toJioFile
import matt.lang.function.Produce
import matt.model.flowlogic.await.ThreadAwaitable
import matt.model.flowlogic.latch.asyncloaded.DelegatedSlot
import matt.nn.deephys.gui.DEEPHY_USER_DATA_DIR
import matt.nn.deephys.load.cache.cachedeleter.CacheDeleter
import matt.nn.deephys.load.cache.raf.EvenlySizedRAFCache
import matt.nn.deephys.load.cache.raf.RAFCacheImpl
import matt.sys.idgen.IDGenerator


object DeephysCacheManager {

    private val DEEPHY_CACHE_DIR = DEEPHY_USER_DATA_DIR.toJioFile().mkFold("Cache").toJioFile()

    private val DATA_SETS_CACHE_DIR = DEEPHY_CACHE_DIR.mkFold("datasets").toJioFile()

    private val oldDatasetCaches = DATA_SETS_CACHE_DIR.listFiles()!!

    val cacheDeleter = CacheDeleter {
        val weirdCaches = DEEPHY_CACHE_DIR.listFiles()!!.filter { it !in listOf(DATA_SETS_CACHE_DIR) }
        weirdCaches + oldDatasetCaches
    }

    private val oldDatasetIDs = oldDatasetCaches.mapNotNull {
        it.name.toIntOrNull()
    }

    private val idGenerator = IDGenerator(taken = oldDatasetIDs)

    @Synchronized
    private fun getNextDatasetID() = idGenerator.next()

    fun newDatasetCache() = DatasetCache(DATA_SETS_CACHE_DIR.mkFold(getNextDatasetID()).toJioFile())


    class DatasetCache(
        folder: FsFile,
    ) {
        val neuronsRAF = RAFCacheImpl(folder["neurons.raf"])
        val activationsRAF = RAFCacheImpl(folder["activations.raf"])
        val pixelsRAF = RAFCacheImpl(folder["pixels.raf"])

    }
}

interface Cacheable {
    val cacheID: Int
}


abstract class FileCaches(
    private val rootCacheFolder: FsFile
) : Caches(), Cacheable {
    private val cacheFold by lazy {
        rootCacheFolder.toJioFile().mkFold("$cacheID")
    }


    abstract inner class CachedFileProp<R : Any> protected constructor() : CachedProp<R>() {

        /*example: "pixels.cbor"*/
        protected abstract val propFileName: String

        private val propFile get() = cacheFold[propFileName].toJioFile()
        final override fun cache(bytes: ByteArray) {
            propFile.writeBytes(bytes)
            lazyWeak {
                decode(propFile.readBytes())
            }
        }
    }


}

abstract class RAFCaches : Caches() {
    abstract inner class CachedRAFProp<R : Any> protected constructor(
        rafCache: EvenlySizedRAFCache,
    ) : CachedProp<R>() {
        val deed by lazy { rafCache.rent() }
        final override fun cache(bytes: ByteArray) {
            deed.write(bytes)
            lazyWeak {
                decode(deed.read())
            }
        }

        private inner class CacherImpl : Cacher {
            private val stream by lazy { deed.outputStream().buffered(2000) }
            override fun finalize() {
                stream.flush()
                lazyWeak {
                    decode(deed.read())
                }
            }

            override fun write(bytes: ByteArray) {
                stream.write(bytes)
            }
        }

        val cacher: Cacher by lazy {
            CacherImpl()
        }
    }
}

interface Cacher {
    fun write(bytes: ByteArray)
    fun finalize()
}

abstract class Caches {
    abstract inner class CachedProp<R : Any> protected constructor() : ThreadAwaitable<R> {
        private val slot = DelegatedSlot<R>()
        final override fun await() = slot.await()
        fun strong(r: R) {
            slot.putGetter { r }
        }

        fun strong(r: Produce<R>) {
            slot.putGetter { r() }
        }

        fun lazyWeak(r: Produce<R>) {
            slot.putLazyWeakGetter { r() }
        }

        abstract fun cache(bytes: ByteArray)
        protected abstract fun decode(bytes: ByteArray): R
    }
}


