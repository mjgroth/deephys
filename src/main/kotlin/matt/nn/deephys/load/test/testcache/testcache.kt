package matt.nn.deephys.load.test.testcache

import matt.caching.compcache.globalman.ComputeCacheContext
import matt.caching.compcache.globalman.RAMComputeCacheManager
import matt.nn.deephys.gui.settings.DeephysSettingsController

@Suppress("unused")
class TestRAMCache(settings: DeephysSettingsController) :
    RAMComputeCacheManager(), ComputeCacheContext {
    override val cacheManager = this
}
