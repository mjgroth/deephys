package matt.nn.deephys.gui.global.tooltip

import javafx.scene.control.ContentDisplay.BOTTOM
import javafx.scene.control.Tooltip
import javafx.util.Duration
import matt.async.queue.QueueThread
import matt.async.queue.QueueThread.SleepType.WHEN_NO_JOBS
import matt.collect.map.lazyMap
import matt.fx.control.tooltip.add
import matt.fx.graphics.fxthread.runLaterReturn
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.lang.sync
import matt.model.flowlogic.controlflowstatement.ControlFlow.CONTINUE
import matt.nn.deephys.gui.draw.draw
import matt.nn.deephys.gui.global.DEEPHY_FONT_DEFAULT
import matt.nn.deephys.model.importformat.DeephyImage
import matt.sys.loopthread.DaemonLoop
import matt.time.dur.ms
import kotlin.time.Duration.Companion.seconds


/*cant have op here since it will operate on the tooltip for other nodes*/
fun NodeWrapper.deephyTooltip(s: String, im: DeephyImage? = null/*, op: Tooltip.()->Unit = {}*/): Tooltip {
  return tooltips[s to im].also {
	add(it)
  }
}

/*a single tooltip can be installed on multiple nodes, (and this seems important for performance)*/
val tooltips = lazyMap<Pair<String, DeephyImage?>, Tooltip> {
  DeephyTooltip(it.first, it.second)
}


private class DeephyTooltip(s: String, im: DeephyImage?): Tooltip(s) {
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
	showDelay = Duration.millis(100.0)
	hideDelay = Duration.millis(1000.0)
	contentDisplay = BOTTOM
	if (im != null) {
	  drawQueue.with {
		ScaledCanvas().apply {
		  draw(im)
		  scale.value = 5.0
		}.node.also {
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
  }
}