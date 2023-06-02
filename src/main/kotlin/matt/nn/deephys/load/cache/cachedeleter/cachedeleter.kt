package matt.nn.deephys.load.cache.cachedeleter

import matt.async.pri.MyThreadPriorities.DELETING_OLD_CACHE
import matt.file.MFile
import matt.lang.NUM_LOGICAL_CORES
import matt.lang.function.Produce
import matt.math.round.ceilInt
import matt.model.flowlogic.await.Awaitable
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import kotlin.concurrent.thread

class CacheDeleter(
    files: Produce<List<MFile>>
) : Awaitable<Unit> {
    private val deleteCachesThreads = LoadedValueSlot<List<Thread>>()

    init {
        thread(
            name = "delete thread generator",
            isDaemon = true,
            priority = DELETING_OLD_CACHE.ordinal
        ) {
            val toDelete = files()
            if (toDelete.isNotEmpty()) {
                val chunks =
                    toDelete.chunked((toDelete.size.toDouble() / NUM_LOGICAL_CORES.toDouble()).ceilInt())
                val ts = chunks.mapIndexed { idx, it ->
                    thread(name = "delete caches $idx", isDaemon = true, priority = DELETING_OLD_CACHE.ordinal) {
                        it.forEach {
                            it.deleteIfExists()
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