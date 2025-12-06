package matt.nn.deephys.gui.dataset.dtab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import matt.compose.controls.tabpane.EnumTabPane
import matt.compose.controls.tabpane.SimpleTabPane
import matt.compose.controls.tabpane.SimpleTabPaneScope
import matt.compose.controls.tabpane.state.SimpleTabPaneController
import matt.compose.graphics.text.Title
import matt.lang.common.unsafeError
import kotlin.enums.enumEntries

@Suppress("unused")
@Composable
fun DeephysTabPane(
    controller: SimpleTabPaneController,
    content: SimpleTabPaneScope.() -> Unit
) {

    unsafeError("This should migrate as basically just a tap pane but in which the tabs are colored a certain color when selected: `DeephysPalette.deephysSelectGradient`")

    SimpleTabPane(controller = controller) {
        content()
    }
}

@Composable
inline fun <reified E: Enum<E>> DeephysEnumTabPane(
    selected: MutableState<E>,
    noinline labels: @Composable (E) -> String,
    noinline contents: @Composable (E) -> Unit
) {

    unsafeError("This should migrate as basically just a tap pane but in which the tabs are colored a certain color when selected: `DeephysPalette.deephysSelectGradient`")

    EnumTabPane(
        debugEntries = enumEntries<E>(),
        selected = selected,
        labels = { Title(labels(it)) },
        contents = contents
    )
}
