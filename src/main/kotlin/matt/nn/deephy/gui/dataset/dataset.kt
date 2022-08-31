package matt.nn.deephy.gui.dataset

import matt.hurricanefx.wrapper.pane.anchor.swapper.Swapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.gui.dataset.byimage.ByImageView
import matt.nn.deephy.gui.dataset.byneuron.ByNeuronView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.Test

enum class DatasetNodeView {
  ByNeuron, ByImage
}

class DatasetNode(
  dataset: Test, viewer: DatasetViewer
): Swapper<DatasetNodeView, RegionWrapper<*>>() {


  private val byNeuronView by lazy { ByNeuronView(dataset, viewer) }
  private val byImageView by lazy { ByImageView(dataset, viewer) }

  init {
	setupSwapping(viewer.view) {
	  when (this) {
		ByNeuron -> byNeuronView
		ByImage  -> byImageView
	  }
	}
  }
}