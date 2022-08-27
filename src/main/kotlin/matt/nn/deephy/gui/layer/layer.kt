package matt.nn.deephy.gui.layer

import matt.hurricanefx.eye.bind.toStringConverter
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.nn.deephy.gui.neuron.NeuronView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.LayerLike
import matt.nn.deephy.model.ResolvedDeephyImage

class LayerView(
  layer: LayerLike,
  images: List<ResolvedDeephyImage>,
  viewer: DatasetViewer
): VBoxWrapper<RegionWrapper<*>>() {
  init {
	val neuronCB = choicebox(property = viewer.neuronSelection, values = layer.neurons) {
	  converter = toStringConverter { "neuron ${it?.index}" }
	}
	hbox<NodeWrapper> {
	  text("neuron: ")
	  +neuronCB
	  visibleAndManagedProp().bind(viewer.boundTo.isNull)
	}
	swapper(neuronCB.valueProperty, nullMessage = "select a neuron") {
	  NeuronView(this, images = images, viewer = viewer)
	}
  }
}