package matt.nn.deephys.gui.global.color

import javafx.scene.paint.CycleMethod.NO_CYCLE
import matt.color.common.rgb
import matt.fx.graphics.style.DarkModeController
import matt.fx.graphics.wrapper.style.gradient.linearGradient
import matt.fx.graphics.wrapper.style.toFXColor
import matt.obs.bind.binding

object DeephysPalette {
    val deephysBlue1 = rgb(0x00bbe2).toFXColor()
    val deephysBlue2 = rgb(0x3360ad).toFXColor()
    val deephysRed1 = rgb(0xf5c39e).toFXColor()
    val deephysRed2 = rgb(0xda1d52).toFXColor()
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

    val tooltipBackground by lazy {
        DarkModeController.darkModeProp.binding {
            if (it) {
                rgb(0x11_11_11).toFXColor()
            } else {
                rgb(0xEE_EE_EE).toFXColor()
            }
        }
    }
}


