package matt.nn.deephys.gui.global.tooltip

import javafx.scene.control.ContentDisplay.BOTTOM
import javafx.util.Duration
import matt.async.queue.QueueWorker
import matt.fx.control.inter.contentDisplay
import matt.fx.control.inter.graphic
import matt.fx.control.wrapper.tooltip.Owner
import matt.fx.control.wrapper.tooltip.TooltipWrapper
import matt.fx.control.wrapper.tooltip.install
import matt.fx.graphics.fxthread.runLaterReturn
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.lang.sync
import matt.model.flowlogic.controlflowstatement.ControlFlow.CONTINUE
import matt.nn.deephys.gui.draw.draw
import matt.nn.deephys.gui.global.DEEPHY_FONT_DEFAULT
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.state.DeephySettings
import matt.sys.loopthread.DaemonLoop
import kotlin.time.Duration.Companion.seconds


/*cant have op here since it will operate on the tooltip for other nodes*/
fun NodeWrapper.deephyTooltip(
  s: String,
  im: DeephyImage<*>? = null
  /*, op: Tooltip.()->Unit = {}*/
): TooltipWrapper {

  if (im == null) {
	return DeephyTooltip(s, null).also {
	  install(it)
	}
  }

  return im.testLoader.testRAMCache.tooltips[im][s]!!.also {
	install(it)
  }
}


class DeephyTooltip(s: String, im: DeephyImage<*>?): TooltipWrapper(s) {
  companion object {
	private val drawQueue = QueueWorker()
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
	  }).sendStartSignal()
	}
  }

  init {

	/*text = if (DeephySettings.showTutorials.value) "$s\t(press escape to close)" else s*/


	font = DEEPHY_FONT_DEFAULT

	var didFirstShow = false

	comfortablyShowForeverUntilEscaped()
	val ms1 = DeephySettings.millisecondsBeforeTooltipsVanish.value
	if (ms1 != 0) {
	  hideDelay = Duration.millis(ms1.toDouble())
	}
//	println("hideDelay1=${hideDelay}")

	node.setOnShown {

	  /*putting this stuff in setOnShown to reduce the amount of CPU and memory resources used by tooltips that never show*/
	  if (!didFirstShow) {
		val ms2 = DeephySettings.millisecondsBeforeTooltipsVanish.value
		if (ms2 != 0) {
		  hideDelay = Duration.millis(ms2.toDouble())
		}
//		println("hideDelay1.5=${hideDelay}")
		DeephySettings.millisecondsBeforeTooltipsVanish.onChangeWithWeak(this) { tt, newMS ->
		  if (newMS == 0) {
			tt.hideDelay = Duration.INDEFINITE
		  } else {
			tt.hideDelay = Duration.millis(newMS.toDouble())
		  }
//		  println("hideDelay1.7=${tt.hideDelay}")
		}
	  }

//	  println("hideDelay2=${hideDelay}")
	  didFirstShow = true


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
	  drawQueue.schedule {
		ScaledCanvas().apply {
		  draw(im)
		  scale.value = 5.0
		}.also {
		  synchronized(runLaterBunch) {
			runLaterBunch += {
			  graphic = it/*v {
				deephyText("(press escape to close this)") {
				  visibleAndManaged = DeephySettings.showTutorials.value
				  runLater {
					fill = Color.GREEN *//*cant be seen otherwise on dark mode*//*
				  }
				}
				+it
			  }*/
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