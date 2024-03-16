package matt.nn.deephys.gui.dataset.dtab

import matt.fx.control.toggle.mech.ToggleMechanism
import matt.fx.control.wrapper.button.toggle.ToggleButtonWrapper
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.nn.deephys.gui.global.color.DeephysPalette
import matt.nn.deephys.gui.global.deephyToggleButton


class DeephysTabPane: VBoxW(childClass = NodeWrapper::class) {

    init {
        isFillWidth = false
    }

    val toggleGroup = ToggleMechanism<Lazy<NodeWrapper>>()

    val tabBar =
        h {
        }

    val contentBox =
        v {
        }

    fun deephysLazyTab(label: String, op: () -> NodeWrapper): ToggleButtonWrapper {
        val lazyContent = lazy { op() }
        return tabBar.deephyToggleButton(label, group = toggleGroup, value = lazyContent) {
            setupSelectionColor(DeephysPalette.deephysSelectGradient)
            selectedProperty.onChange {
                if (it) {
                    this@DeephysTabPane.contentBox.apply {
                        clear()
                        +lazyContent.value
                    }
                }
            }
        }
    }
}

