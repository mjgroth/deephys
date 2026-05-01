package matt.nn.deephys.gui.viewer.tutorial.bind

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import matt.compose.controls.check.MyCheckbox
import matt.lang.err.NEVER
import matt.nn.deephys.gui.dsetsbox.BIND_BUTTON_NAME
import matt.nn.deephys.gui.dsetsbox.NORMALIZER_BUTTON_NAME
import matt.nn.deephys.gui.global.DeephyActionText
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.SpacerWithOldFxSize
import matt.nn.deephys.gui.viewer.DatasetViewerState

@Composable
fun BindTutorial(viewer: DatasetViewerState) {
    Column {
        if (
            viewer.showTutorials.value &&
            viewer.numViewers.value > 1 &&
            viewer.outerBoundDSet.value != (viewer) &&
            (viewer.isUnboundToDSet.value || viewer.normalizer.value == null)
        ) {
            SpacerWithOldFxSize()
            DeephysText(s = "In order to visualize this dataset in comparison to other datasets:")
            Row {
                SpacerWithOldFxSize()
                Column {
                    Row {
                        MyCheckbox(
                            "$BIND_BUTTON_NAME one dataset",
                            enabled = false,
                            checked = viewer.outerBoundDSet.value != null,
                            onCheckedChange = { NEVER }
                        )
                        SpacerWithOldFxSize()
                        DeephyActionText("show me how") {
                            viewer.outerBox.flashBindButtons()
                        }
                    }
                    Row {
                        MyCheckbox(
                            "Select one dataset as $NORMALIZER_BUTTON_NAME",
                            enabled = false,
                            checked = viewer.normalizer.value != null,
                            onCheckedChange = { NEVER }
                        )
                        SpacerWithOldFxSize()
                        DeephyActionText("show me how") {
                            viewer.outerBox.flashOODButtons()
                        }
                    }
                }
            }
        }
    }
}
