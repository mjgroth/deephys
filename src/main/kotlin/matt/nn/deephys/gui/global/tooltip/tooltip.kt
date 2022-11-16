package matt.nn.deephys.gui.global.tooltip

import javafx.scene.control.ContentDisplay.BOTTOM
import matt.async.queue.QueueThread
import matt.async.queue.QueueThread.SleepType.WHEN_NO_JOBS
import matt.collect.map.lazyMap
import matt.collect.weak.lazyWeakMap
import matt.fx.control.inter.contentDisplay
import matt.fx.control.inter.graphic
import matt.fx.control.wrapper.tooltip.Owner
import matt.fx.control.wrapper.tooltip.TooltipWrapper
import matt.fx.control.wrapper.tooltip.install
import matt.fx.graphics.fxthread.runLaterReturn
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.lang.sync
import matt.lang.weak.WeakRef
import matt.model.flowlogic.controlflowstatement.ControlFlow.CONTINUE
import matt.nn.deephys.gui.draw.draw
import matt.nn.deephys.gui.global.DEEPHY_FONT_DEFAULT
import matt.nn.deephys.model.importformat.DeephyImage
import matt.sys.loopthread.DaemonLoop
import matt.time.dur.ms
import kotlin.time.Duration.Companion.seconds


/*cant have op here since it will operate on the tooltip for other nodes*/
fun NodeWrapper.deephyTooltip(s: String, im: DeephyImage? = null/*, op: Tooltip.()->Unit = {}*/): TooltipWrapper {
  return tooltips[im][s]!!.also {
	install(it)
  }
}

/*a single tooltip can be installed on multiple nodes, (and this seems important for performance)*/
val tooltips = lazyWeakMap<DeephyImage?, Map<String, TooltipWrapper>> { im ->

  if (im == null) {
	lazyMap { str ->
	  DeephyTooltip(str, null)
	}
  } else {
	val weakIm = WeakRef(im) /*prevents the tooltip map from leaking DeephyImages into memory*/
	lazyMap { str ->
	  DeephyTooltip(str, weakIm.deref()!!) /*this reference should always return non-null as long as the image is still being used.*/
	}
  }

}


private class DeephyTooltip(s: String, im: DeephyImage?): TooltipWrapper(s) {
  companion object {
	private val drawQueue = QueueThread(sleepPeriod = 250.ms, sleepType = WHEN_NO_JOBS)
	private val runLaterBunch = mutableSetOf<()->Unit>()

	init {
	  DaemonLoop(1.seconds, op = {
		runLaterBunch.sync {
		  if (runLaterBunch.isNotEmpty()) {
			runLaterReturn {
			  runLaterBunch.forEach {
				it()
			  }
			  runLaterBunch.clear()
			}
		  }
		}
		CONTINUE
	  }).start()
	}
  }

  init {
	font = DEEPHY_FONT_DEFAULT
	comfortableShowAndHideSettingsForMatt()

	node.setOnShown {
	  val screenMaxX = screen!!.bounds.maxX
	  val screenMaxY = screen!!.bounds.maxY

	  x = when {
		screenMaxX > x + width + 50.0 -> x + 50.0
		screenMaxX > x + width + 10.0 -> screenMaxX - width
		else                          -> screenMaxX - width*2
	  }


	  y = when {
		screenMaxY > y + height + 50.0 -> y + 50.0
		screenMaxY > y + height + 10.0 -> screenMaxY - height
		else                           -> screenMaxY - height*2
	  }
	}

	contentDisplay = BOTTOM
	if (im != null) {
	  drawQueue.with {
		ScaledCanvas().apply {
		  draw(im)
		  scale.value = 5.0
		}.also {
		  synchronized(runLaterBunch) {
			runLaterBunch += {
			  graphic = it
			}
			if (runLaterBunch.size >= 100) {
			  runLaterReturn {
				runLaterBunch.forEach {
				  it()
				}
				runLaterBunch.clear()
			  }
			}
		  }
		}
	  }
	}
	sendMouseEventsTo = Owner
  }
}