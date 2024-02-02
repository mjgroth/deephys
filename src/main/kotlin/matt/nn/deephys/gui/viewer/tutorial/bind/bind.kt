package matt.nn.deephys.gui.viewer.tutorial.bind

import matt.fx.control.wrapper.checkbox.checkbox
import matt.fx.graphics.wrapper.node.visibleAndManagedWhen
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox.Companion.BIND_BUTTON_NAME
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox.Companion.NORMALIZER_BUTTON_NAME
import matt.nn.deephys.gui.global.deephyActionText
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.obs.bindings.bool.and
import matt.obs.bindings.bool.or
import matt.obs.bindings.comp.gt

class BindTutorial(viewer: DatasetViewer): VBoxW() {
    init {
        visibleAndManagedWhen {
            viewer.showTutorials and
                viewer.numViewers.gt(1) and
                viewer.outerBoundDSet.neq(viewer) and
                (viewer.isUnboundToDSet or viewer.normalizer.isNull)
        }
        spacer()
        deephysText("In order to visualize this dataset in comparison to other datasets:")
        h {
            spacer()
            v {
                h {
                    checkbox("$BIND_BUTTON_NAME one dataset") {
                        isDisable = true
                        selectedProperty.bind(viewer.outerBoundDSet.isNotNull)
                    }
                    spacer()
                    deephyActionText("show me how") {
                        viewer.outerBox.flashBindButtons()
                    }
                }
                h {
                    checkbox("Select one dataset as $NORMALIZER_BUTTON_NAME") {
                        isDisable = true
                        selectedProperty.bind(viewer.normalizer.isNotNull)
                    }
                    spacer()
                    deephyActionText("show me how") {
                        viewer.outerBox.flashOODButtons()
                    }
                }
            }
        }
    }
}
