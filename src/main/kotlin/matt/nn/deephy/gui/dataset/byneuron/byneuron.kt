package matt.nn.deephy.gui.dataset.byneuron

import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.nn.deephy.gui.layer.LayerView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.Dataset

class ByNeuronView(
  dataset: Dataset,
  viewer: DatasetViewer
): VBoxWrapper<RegionWrapper<*>>() {
  init {
	val layerCB = choicebox(property = viewer.layerSelection, values = dataset.resolvedLayers)
	hbox<NodeWrapper> {
	  text("layer: ")
	  +layerCB
	  visibleAndManagedProp().bind(viewer.boundTo.isNull)
	}
	viewer.layerSelection.onChange {
	  println("viewer(of ${dataset.datasetName}).layerSelection=${it}")
	}
	layerCB.valueProperty.onChange {
	  println("layerCB(of ${dataset.datasetName}).value=${it}")
	}
	swapper(layerCB.valueProperty, nullMessage = "select a layer") {
	  LayerView(this, dataset.resolvedImages, viewer)
	}
  }
}