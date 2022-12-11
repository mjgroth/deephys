package matt.nn.deephys.gui.dataset.byimage.mult

import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.nn.deephys.calc.UniqueContents
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.neuronListViewSwapper
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.DeephyImage
import matt.obs.math.op.times

class MultipleImagesView(
  viewer: DatasetViewer,
  images: List<DeephyImage>,
  title: String,
  tooltip: String
): VBoxWrapperImpl<NW>() {
  companion object {
	private const val MAX_IMS = 25
  }

  init {
	deephyText("$title (${images.size})").apply {
	  subtitleFont()
	  deephyTooltip("$tooltip (first $MAX_IMS)")
	}
	+ImageFlowPane(viewer).apply {
	  prefWrapLengthProperty.bind(viewer.widthProperty*0.4)
	  images.take(MAX_IMS).forEach {
		+DeephyImView(it, viewer)
	  }
	}
	neuronListViewSwapper(
	  viewer = viewer,
	  contents = UniqueContents(images)
	)
  }
}