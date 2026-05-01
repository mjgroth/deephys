package matt.nn.deephys.load.cache.cachedeleter

import matt.async.pri.MyThreadPriority.DELETING_OLD_CACHE
import matt.async.thread.namedThread
import matt.file.construct.toJioFile
import matt.lang.function.Produce
import matt.model.flowlogic.await.ThreadAwaitable
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.model.k.file.file.FsFile
import matt.prim.pint.ceilInt
import matt.sys.j.runtime.NUM_LOGICAL_CORES

class CacheDeleter(
    files: Produce<List<FsFile>>
) : ThreadAwaitable<Unit> {
    private val deleteCachesThreads = LoadedValueSlot<List<Thread>>()

    init {
        val _ =
            namedThread(
                name = "delete thread generator",
                isDaemon = true,
                priority = DELETING_OLD_CACHE.ordinal
            ) {
                val toDelete = files()
                if (toDelete.isNotEmpty()) {
                    val chunks =
                        toDelete.chunked((toDelete.size.toDouble() / NUM_LOGICAL_CORES.toDouble()).ceilInt())
                    val ts =
                        chunks.mapIndexed { idx, it ->
                            namedThread(name = "delete caches $idx", isDaemon = true, priority = DELETING_OLD_CACHE.ordinal) {
                                it.forEach {
                                    it.toJioFile().deleteIfExists()
                                }
                            }
                        }
                    deleteCachesThreads.putLoadedValue(ts)
                } else {
                    deleteCachesThreads.putLoadedValue(listOf())
                }
            }
    }

    override fun await() {
        deleteCachesThreads.await().forEach {
            it.join()
        }
    }
}
