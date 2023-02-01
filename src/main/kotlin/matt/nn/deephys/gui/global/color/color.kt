package matt.nn.deephys.gui.global.color

import javafx.scene.paint.CycleMethod.NO_CYCLE
import matt.color.hexToAwtColor
import matt.fx.graphics.wrapper.style.gradient.linearGradient
import matt.fx.graphics.wrapper.style.toFXColor

object DeephysPalette {
  val deephysBlue1 = hexToAwtColor("#00bbe2").toFXColor()
  val deephysBlue2 = hexToAwtColor("#3360ad").toFXColor()
  val deephysRed1 = hexToAwtColor("#f5c39e").toFXColor()
  val deephysRed2 = hexToAwtColor("#da1d52").toFXColor()
  val deephysSelectGradient by lazy {
	linearGradient {
	  startX = 0.0
	  startY = 0.5
	  endX = 1.0
	  endY = 0.5
	  cycleMethod = NO_CYCLE
	  stop(
		0.0,
		/*Color.YELLOW.deriveColor(0.0, 1.0, 1.0, 0.5)*/
		deephysBlue1
	  )
	  stop(
		1.0,
		/*Color.TRANSPARENT*/
		deephysBlue2
	  )
	}
  }
}


