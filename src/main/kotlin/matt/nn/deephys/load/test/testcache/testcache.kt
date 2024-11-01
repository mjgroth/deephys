package matt.nn.deephys.load.test.testcache

import matt.caching.compcache.ComputeCacheContext
import matt.caching.compcache.globalman.RAMComputeCacheManager
import matt.nn.deephys.gui.settings.DeephysSettingsController

class TestRAMCache(settings: DeephysSettingsController) : RAMComputeCacheManager(), ComputeCacheContext {
    override val cacheManager = this
}
