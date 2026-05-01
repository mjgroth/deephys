package matt.nn.deephys.gui.dataset.byimage.mult

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import matt.lang.controlflow.go
import matt.model.k.log.Logger
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.neuronListViewSwapper
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.load.test.PostDtypeTestLoader
import matt.nn.deephys.model.importformat.im.DeephyImage

private const val MAX_IMS = 25
@Composable
context(_: Logger)
fun <A: Number> MultipleImagesView(
    viewer: DatasetViewerState,
    images: List<DeephyImage<A>>,
    post: PostDtypeTestLoader<A>,
    title: String?,
    tooltip: String,
    fade: Boolean = true,
    settings: DeephysSettingsController,
    viewerWidth: Dp
) {
    DeephysTooltipArea(settings, "$tooltip (first $MAX_IMS)") {
        Column {

            title?.go {
                DeephysText(
                    s = "$title (${images.size})",
                    style = subtitleFont()
                )
            }
            ImageFlowPane(
                viewer,
                prefWrapLengthProperty = viewerWidth * 0.4f,
                gap = 0.dp /*only because I THINK this path didn't include a g\"gap\" in FX*/
            ) {
                images.take(MAX_IMS).forEach {
                    DeephyImView(it, viewer, settings = settings)
                }
                if (images.size > MAX_IMS) {
                    DeephysText(s = "(+${images.size - MAX_IMS} more)")
                }
            }
            neuronListViewSwapper(
                viewer = viewer,
                contents = images.toSet(),
                fade = fade,
                settings = settings,
                postDtypeTestLoader = post,
                viewerWidth = viewerWidth
            )
        }
    }
}
