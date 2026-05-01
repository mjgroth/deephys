package matt.nn.deephys.gui.global.color

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import matt.color.rgb
import matt.compose.graphics.color.toComposeColor
import matt.lang.err.unsafeError
import matt.obs.prop.writable.BindableProperty

object DeephysPalette {
    val deephysBlue1 = rgb(0x00bbe2).toComposeColor()
    val deephysBlue2 = rgb(0x3360ad).toComposeColor()
    @Suppress("unused")
    val deephysRed1 = rgb(0xf5c39e).toComposeColor()
    @Suppress("unused")
    val deephysRed2 = rgb(0xda1d52).toComposeColor()
    @Suppress("unused")
    val deephysSelectGradient by lazy {
        Brush.linearGradient(
            0f to deephysBlue1,
            1f to deephysBlue2,
            start =
                Offset(
                    x = 0.0f,
                    y = 0.5f
                ),
            end =
                Offset(
                    x = 1.0f,
                    y = 0.5f
                )
        )
    }

    val tooltipBackground by lazy {
        unsafeError(
            """
                      DarkModeController.darkModeProp.binding {
                if (it) {
                    rgb(0x11_11_11).toFXColor()
                } else {
                    rgb(0xEE_EE_EE).toFXColor()
                }
            }
            """.trimIndent()
        )
        BindableProperty(rgb(0x11_11_11).toComposeColor())
    }
}
