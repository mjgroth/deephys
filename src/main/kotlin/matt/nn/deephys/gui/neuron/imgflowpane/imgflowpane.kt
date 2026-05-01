package matt.nn.deephys.gui.neuron.imgflowpane

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import matt.compose.graphics.ComposeContent
import matt.nn.deephys.gui.viewer.DatasetViewerState

@Suppress("UnusedParameter")
@Composable
fun ImageFlowPane(
    viewer: DatasetViewerState,
    prefWrapLengthProperty: Dp,
    gap: Dp,
    content: ComposeContent

) {
    FlowRow(
        modifier = Modifier.width(prefWrapLengthProperty),
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        content()
    }
}
