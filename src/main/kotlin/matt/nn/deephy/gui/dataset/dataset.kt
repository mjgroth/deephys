package matt.nn.deephy.gui.dataset

import matt.hurricanefx.wrapper.pane.anchor.swapper.Swapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.nn.deephy.gui.DatasetViewer
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.gui.dataset.byimage.ByImageView
import matt.nn.deephy.gui.dataset.byneuron.ByNeuronView
import matt.nn.deephy.model.Dataset
import matt.obs.prop.BindableProperty

enum class DatasetNodeView {
  ByNeuron, ByImage
}

class DatasetNode(
  dataset: Dataset, viewer: DatasetViewer
): Swapper<DatasetNodeView, RegionWrapper<*>>() {

  val view = BindableProperty(ByNeuron)

  private val byNeuronView by lazy { ByNeuronView(dataset, viewer, this@DatasetNode) }
  private val byImageView by lazy { ByImageView(dataset, viewer, this@DatasetNode) }

  init {
	setupSwapping(view) {
	  when (this) {
		ByNeuron -> byNeuronView
		ByImage  -> byImageView
	  }
	}
  }
}