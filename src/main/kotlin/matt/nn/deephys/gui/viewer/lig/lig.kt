@file:Suppress("unused")

package matt.nn.deephys.gui.viewer.lig

import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import matt.rstruct.loader.desktop.systemResourceLoader


object LigatureFont {
    private val url = systemResourceLoader().resourceURL("font/FiraCode-Bold.ttf").toString()
    val ligatureFontFont = Font(url)
    val ligatureFontSize = 45.0.sp
}
