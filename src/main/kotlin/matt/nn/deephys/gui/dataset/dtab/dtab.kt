@file:Suppress("unused")

package matt.nn.deephys.gui.dataset.dtab

import androidx.compose.runtime.Composable
import matt.compose.controls.tabpane.SimpleTabPane
import matt.compose.controls.tabpane.SimpleTabPaneScope
import matt.lang.common.unsafeErr

@Composable
fun DeephysTabPane(
    content: SimpleTabPaneScope.() -> Unit
) {

    unsafeErr("This should migrate as basically just a tap pane but in which the tabs are colored a certain color when selected: `DeephysPalette.deephysSelectGradient`")

    SimpleTabPane {
        content()
    }
}

