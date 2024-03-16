package matt.nn.deephys.gui.dataset.byimage.mult

import matt.collect.set.contents.Contents
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.lang.common.go
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.neuronListViewSwapper
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.PostDtypeTestLoader
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.obs.math.op.times

class MultipleImagesView<A: Number>(
    viewer: DatasetViewer,
    images: List<DeephyImage<A>>,
    post: PostDtypeTestLoader<A>,
    title: String?,
    tooltip: String,
    fade: Boolean = true,
    override val settings: DeephysSettingsController
): VBoxWrapperImpl<NW>(childClass = NW::class), DeephysNode {
    companion object {
        private const val MAX_IMS = 25
    }

    init {
        val memSafeSettings = settings
        title?.go {
            deephysText("$title (${images.size})").apply {
                subtitleFont()
            }
        }
        veryLazyDeephysTooltip(
            "$tooltip (first $MAX_IMS)",
            settings = memSafeSettings
        )
        +ImageFlowPane(viewer).apply {
            prefWrapLengthProperty.bindWeakly(viewer.widthProperty*0.4)
            images.take(MAX_IMS).forEach {
                +DeephyImView(it, viewer, settings = memSafeSettings).apply {
                }
            }
            if (images.size > MAX_IMS) {
                deephysText("(+${images.size - MAX_IMS} more)")
            }
        }
        neuronListViewSwapper(
            viewer = viewer,
            contents = Contents(images),
            fade = fade,
            settings = memSafeSettings,
            postDtypeTestLoader = post
        )
    }
}
