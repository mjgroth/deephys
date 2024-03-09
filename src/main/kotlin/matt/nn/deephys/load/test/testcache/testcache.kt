package matt.nn.deephys.load.test.testcache

import matt.caching.compcache.ComputeCacheContext
import matt.caching.compcache.globalman.RAMComputeCacheManager
import matt.collect.map.lazyMap
import matt.collect.weak.lazy.lazyWeakMap
import matt.lang.weak.weak
import matt.nn.deephys.gui.global.tooltip.DeephyTooltip
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.model.importformat.im.DeephyImage

class TestRAMCache(settings: DeephysSettingsController) : RAMComputeCacheManager(), ComputeCacheContext {
    override val cacheManager = this
    /*a single tooltip can be installed on multiple nodes, (and this seems important for performance)*/
    val tooltips =
        run {
            lazyWeakMap<DeephyImage<*>, Map<String, DeephyTooltip>> { im ->
                val weakIm = weak(im) /*prevents the tooltip map from leaking DeephyImages into memory*/
                lazyMap { str ->
                    DeephyTooltip(
                        str, weakIm.deref()!!, settings = settings
                    ) /*this reference should always return non-null as long as the image is still being used.*/
                }
            }
        }
}
