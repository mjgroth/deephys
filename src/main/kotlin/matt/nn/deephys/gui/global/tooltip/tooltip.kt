package matt.nn.deephys.gui.global.tooltip

import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.ContentDisplay.BOTTOM
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.util.Duration
import matt.fx.control.inter.contentDisplay
import matt.fx.control.inter.graphic
import matt.fx.control.wrapper.label.LabelWrapper
import matt.fx.control.wrapper.tooltip.Owner
import matt.fx.control.wrapper.tooltip.fixed.FixedTooltipWrapper
import matt.fx.control.wrapper.tooltip.fixed.install
import matt.fx.control.wrapper.wrapped.wrapped
import matt.fx.graphics.fxthread.runLater
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.node.attachTo
import matt.fx.graphics.wrapper.node.shape.rect.rectangle
import matt.fx.graphics.wrapper.pane.stack.StackPaneW
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.fx.node.proto.infosymbol.InfoSymbol
import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.fx.node.tex.TexNodeFactory
import matt.lang.function.DSL
import matt.lang.function.Produce
import matt.lang.weak.MyWeakRef
import matt.nn.deephys.gui.draw.draw
import matt.nn.deephys.gui.global.DEEPHYS_FONT_DEFAULT
import matt.nn.deephys.gui.global.color.DeephysPalette
import matt.nn.deephys.gui.settings.DEFAULT_BIG_IMAGE_SCALE
import matt.nn.deephys.gui.settings.DeephySettings
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.obs.bindings.str.ObsS
import java.lang.ref.WeakReference

fun NodeWrapper.veryLazyDeephysTooltip(text: String, im: MyWeakRef<out DeephyImage<out Number>>) {

  val handler = object: EventHandler<MouseEvent> {
	override fun handle(event: MouseEvent) {    /*kinda works like a weak ref*/
	  val target = (event.target as Node)
	  target.wrapped().also {
		it.deephyTooltip(text, im.deref()!!)
		it.removeEventHandler(MouseEvent.MOUSE_ENTERED, this)
	  }
	}
  }

  addEventHandler(MouseEvent.MOUSE_ENTERED, handler)
}

fun NodeWrapper.veryLazyDeephysTooltip(text: String) {

  val handler = object: EventHandler<MouseEvent> {
	override fun handle(event: MouseEvent) {    /*kinda works like a weak ref*/
	  val target = (event.target as Node)
	  target.wrapped().also {
		it.deephyTooltip(text)
		it.removeEventHandler(MouseEvent.MOUSE_ENTERED, this)
	  }

	}
  }

  addEventHandler(MouseEvent.MOUSE_ENTERED, handler)
}

fun NodeWrapper.veryLazyDeephysTooltip(op: Produce<String>) {
  val handler = object: EventHandler<MouseEvent> {
	override fun handle(event: MouseEvent) {    /*kinda works like a weak ref*/
	  val target = (event.target as Node)
	  target.wrapped().also {
		it.deephyTooltip(op())
		it.removeEventHandler(MouseEvent.MOUSE_ENTERED, this)
	  }
	}
  }
  addEventHandler(MouseEvent.MOUSE_ENTERED, handler)
}

fun NodeWrapper.veryLazyDeephysTexTooltip(getCode: Produce<String>) =
  veryLazyDeephysTooltipWithNode(/*darkBG = true*/) {
	deephysTexNodeFactory.toCanvas(
	  getCode()
	) ?: TextWrapper("error")
  }

fun NodeWrapper.veryLazyDeephysTooltipWithNode(/*darkBG: Boolean = false, */op: Produce<NodeWrapper>) {
  val handler = object: EventHandler<MouseEvent> {
	override fun handle(event: MouseEvent) {    /*kinda works like a weak ref*/
	  val target = (event.target as Node)
	  target.wrapped().also {        /*if (darkBG) {
		  //		  node.style =
		  //			"""-fx-background: black; -fx-background-color: black"""
		  *//*node.scene.root.style = "-fx-background: black; -fx-background-color: black"*//*
		}*/
		it.deephyTooltip("").apply {
		  contentNode.theLabel.graphic = op()        /*  if (darkBG) {
			  thread {
				sleep(1.seconds)
				runLater {
				  runLater {
					val scn = node.scene
					scn.fill = Color.BLACK
					val reg = (scn.root as Region)
					reg.background = backgroundFromColor(Color.BLACK)
					val pan = (reg.childrenUnmodifiable[0] as Pane)
					pan.background = backgroundFromColor(Color.BLACK)
					reg.style = """-fx-background: black; -fx-background-color: black"""
					pan.style = """-fx-background: black; -fx-background-color: black"""
					val borderAmount = 10.0
					val vbx = (theLabel.graphic as VBoxWrapperImpl<*>)
					vbx.background = backgroundFromColor(Color.BLACK)
					vbx.padding = Insets(borderAmount)
					vbx.border = Border(
					  BorderStroke(
						DeephysPalette.deephysBlue1,
						BorderStrokeStyle.SOLID,
						CornerRadii(10.0),
						BorderWidths(borderAmount)
					  )
					)
				  }
				}
			  }

			}*/
		}
		it.removeEventHandler(MouseEvent.MOUSE_ENTERED, this)
	  }
	}
  }
  addEventHandler(MouseEvent.MOUSE_ENTERED, handler)
}

val deephysTexNodeFactory by lazy {
  TexNodeFactory(scale = 1.0)
}


/*cant have op here since it will operate on the matt.fx.control.wrapper.tooltip.fixed.tooltip for other nodes*/
fun NodeWrapper.deephyTooltip(
  s: String, im: DeephyImage<*>? = null/*, op: Tooltip.()->Unit = {}*/
): DeephyTooltip {

  if (im == null) {
	return DeephyTooltip(s, null).also {
	  install(it)
	}
  }

  return im.testLoader.testRAMCache.tooltips[im][s]!!.also {
	install(it)
  }
}


fun NodeWrapper.deephyTooltip(
  s: ObsS,
): FixedTooltipWrapper {

  return DeephyTooltip(s.value, null).also {
	install(it)
	it.contentNode.theLabel.textProperty.bindWeakly(s)
  }

}


class DeephysTooltipContent(s: String): StackPaneW() {
  val theLabel = LabelWrapper(s).apply {
	contentDisplay = BOTTOM
	font = DEEPHYS_FONT_DEFAULT
	padding = Insets(10.0)
  }

  init {    /*thread{
	  sleep(1.seconds)*/
	runLater {
	  backgroundFill = Color.WHITE        /*backgroundProperty.bindWeakly(DeephysPalette.tooltipBackground)*/
	}    /*}*/

	rectangle {
	  stroke = DeephysPalette.deephysBlue2
	  fillProperty.bindWeakly(DeephysPalette.tooltipBackground)
	  heightProperty.bind(this@DeephysTooltipContent.theLabel.heightProperty)
	  widthProperty.bind(this@DeephysTooltipContent.theLabel.widthProperty)

	}

	+theLabel
  }

}

class DeephyTooltip(s: String, im: DeephyImage<*>?): FixedTooltipWrapper() {
  companion object {    /*private val drawQueue = QueueWorker()
	private val runLaterBunch = mutableSetOf<()->Unit>()*/

	/*	init {


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
		}*/
  }


  val contentNode = DeephysTooltipContent(s)

  init {

	/*text = if (DeephySettings.showTutorials.value) "$s\t(press escape to close)" else s*/

	//	contentProperty

	content = contentNode


	var didFirstShow = false

	comfortablyShowForeverUntilEscaped()
	val ms1 = DeephySettings.millisecondsBeforeTooltipsVanish.value
	if (ms1 != 0) {
	  hideDelay = Duration.millis(ms1.toDouble())
	}    //	println("hideDelay1=${hideDelay}")

	val weakIm = im?.let { WeakReference(it) }



	node.setOnShowing {

	  /*putting this stuff in setOnShown to reduce the amount of CPU and memory resources used by tooltips that never show*/
	  if (!didFirstShow) {
		val ms2 = DeephySettings.millisecondsBeforeTooltipsVanish.value
		if (ms2 != 0) {
		  hideDelay = Duration.millis(ms2.toDouble())
		}        //		println("hideDelay1.5=${hideDelay}")
		DeephySettings.millisecondsBeforeTooltipsVanish.onChangeWithWeak(this) { tt, newMS ->
		  if (newMS == 0) {
			tt.hideDelay = Duration.INDEFINITE
		  } else {
			tt.hideDelay = Duration.millis(newMS.toDouble())
		  }        //		  println("hideDelay1.7=${tt.hideDelay}")
		}
		val derefedIm = weakIm?.get()
		if (derefedIm != null) {


		  contentNode.theLabel.graphic = ScaledCanvas().apply {
			draw(derefedIm)
			scale.value = DEFAULT_BIG_IMAGE_SCALE/derefedIm.widthMaybe
		  }

		  /*  drawQueue.schedule {
			  ScaledCanvas().apply {
				draw(derefedIm)
				scale.value = DEFAULT_BIG_IMAGE_SCALE/derefedIm.widthMaybe
			  }.also {
				synchronized(runLaterBunch) {
				  runLaterBunch += {
					graphic = it*//*v {
				deephyText("(press escape to close this)") {
				  visibleAndManaged = DeephySettings.showTutorials.value
				  runLater {
					fill = Color.GREEN *//**//*cant be seen otherwise on dark mode*//**//*
				  }
				}
				+it
			  }*//*
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
		  }*/


		}        /*label.contentDisplay = BOTTOM*/
		sendMouseEventsTo = Owner
	  }

	  //	  println("hideDelay2=${hideDelay}")
	  didFirstShow = true


	}
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

  }
}

fun NW.deephysInfoSymbol(text: ObsS, op: DSL<DeephysInfoSymbol> = {}) = DeephysInfoSymbol(text.value).attachTo(this) {
  textProperty.bind(text)
  op()
}

fun NW.deephysInfoSymbol(text: String, op: DSL<DeephysInfoSymbol> = {}) = DeephysInfoSymbol(text).attachTo(this, op)



class DeephysInfoSymbol(info: String): InfoSymbol(info) {
  override fun buildTooltipGraphic(info: String) = DeephysTooltipContent(info)
  val textProperty get() = (content as DeephysTooltipContent).theLabel.textProperty
  val fontProperty get() = (content as DeephysTooltipContent).theLabel.fontProperty
}


fun NW.deephysWarningSymbol(text: ObsS, op: DSL<DeephysWarningSymbol> = {}) = DeephysWarningSymbol(text.value).attachTo(this) {
  textProperty.bind(text)
  op()
}

fun NW.deephysWarningSymbol(text: String, op: DSL<DeephysWarningSymbol> = {}) = DeephysWarningSymbol(text).attachTo(this, op)



class DeephysWarningSymbol(warning: String): WarningSymbol(warning) {
  override fun buildTooltipGraphic(warning: String) = DeephysTooltipContent(warning)
  val textProperty get() = (content as DeephysTooltipContent).theLabel.textProperty
  val fontProperty get() = (content as DeephysTooltipContent).theLabel.fontProperty
}



