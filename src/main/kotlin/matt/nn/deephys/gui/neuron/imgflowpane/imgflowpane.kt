package matt.nn.deephys.gui.neuron.imgflowpane

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import matt.compose.graphics.Compose
import matt.nn.deephys.gui.viewer.DatasetViewerState

@Suppress("UnusedParameter")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageFlowPane(
    viewer: DatasetViewerState,
    prefWrapLengthProperty: Dp,
    content: Compose
) {
    FlowRow(
        modifier = Modifier.width(prefWrapLengthProperty)
    ) {
        content()
    }
}
