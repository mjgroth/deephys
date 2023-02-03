package matt.nn.deephys.load.test.testcache

import matt.caching.compcache.globalman.RAMComputeCacheManager
import matt.collect.map.lazyMap
import matt.collect.weak.lazyWeakMap
import matt.lang.weak.MyWeakRef
import matt.nn.deephys.gui.global.tooltip.DeephyTooltip
import matt.nn.deephys.model.importformat.im.DeephyImage

class TestRAMCache: RAMComputeCacheManager() {
  /*a single matt.fx.control.wrapper.tooltip.fixed.tooltip can be installed on multiple nodes, (and this seems important for performance)*/
  val tooltips = lazyWeakMap<DeephyImage<*>, Map<String, DeephyTooltip>> { im ->
	val weakIm = MyWeakRef(im) /*prevents the matt.fx.control.wrapper.tooltip.fixed.tooltip map from leaking DeephyImages into memory*/
	lazyMap { str ->
	  DeephyTooltip(
		str, weakIm.deref()!!
	  ) /*this reference should always return non-null as long as the image is still being used.*/
	}
  }
}