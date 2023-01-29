package matt.nn.deephys.gui.dataset.byimage.mult

import matt.collect.set.contents.Contents
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.neuronListViewSwapper
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.obs.math.op.times

class MultipleImagesView<A: Number>(
  viewer: DatasetViewer,
  images: List<DeephyImage<A>>,
  title: String,
  tooltip: String,
  fade: Boolean = true,
): VBoxWrapperImpl<NW>() {
  companion object {
	private const val MAX_IMS = 25
  }

  init {
	deephyText("$title (${images.size})").apply {
	  subtitleFont()
	  veryLazyDeephysTooltip("$tooltip (first $MAX_IMS)")
	}
	+ImageFlowPane(viewer).apply {
	  prefWrapLengthProperty.bind(viewer.widthProperty*0.4)
	  images.take(MAX_IMS).forEach {
		+DeephyImView(it, viewer).apply {
		  //		  scale.bind(viewer.smallImageScale / it.widthMaybe)
		}
	  }
	}
	neuronListViewSwapper(
	  viewer = viewer,
	  contents = Contents(images),
	  fade=fade
	)
  }
}