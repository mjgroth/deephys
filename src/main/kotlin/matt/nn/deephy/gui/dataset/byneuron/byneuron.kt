package matt.nn.deephy.gui.dataset.byneuron

import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.nn.deephy.gui.layer.LayerView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.Test

class ByNeuronView(
  dataset: Test,
  viewer: DatasetViewer
): VBoxWrapper<RegionWrapper<*>>() {
  init {
	val layerCB = choicebox(property = viewer.layerSelection, values = viewer.model.resolvedLayers) {
	  valueProperty.onChange {
		println("layerCB value changed to $it")
	  }
	}
	hbox<NodeWrapper> {
	  text("layer: ")
	  +layerCB
	  visibleAndManagedProp.bind(viewer.isUnboundToDSet)
	}
	viewer.layerSelection.onChange {
	  println("viewer(of ${dataset.name}).layerSelection=${it}")
	}
	layerCB.valueProperty.onChange {
	  println("layerCB(of ${dataset.name}).value=${it}")
	}
	swapper(layerCB.valueProperty, nullMessage = "select a layer") {
	  println("making LayerView")
	  LayerView(this, dataset.resolvedImages, viewer)
	}
  }
}