package matt.nn.deephy.gui.layer

import matt.hurricanefx.eye.bind.toStringConverter
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.nn.deephy.gui.DatasetViewer
import matt.nn.deephy.gui.dataset.DatasetNode
import matt.nn.deephy.gui.neuron.NeuronView
import matt.nn.deephy.model.DeephyImage
import matt.nn.deephy.model.Layer

class LayerView(
  layer: Layer,
  images: List<DeephyImage>,
  viewer: DatasetViewer,
  dsNode: DatasetNode
): VBoxWrapper<RegionWrapper<*>>() {
  init {
	val neuronCB = choicebox(property = viewer.neuronSelection, values = layer.neurons.withIndex().toList()) {
	  converter = toStringConverter { "neuron ${it?.index}" }
	}
	hbox<NodeWrapper> {
	  text("neuron: ")
	  +neuronCB
	  visibleAndManagedProp().bind(viewer.bound.not())
	}
	swapper(neuronCB.valueProperty, nullMessage = "select a neuron") {
	  NeuronView(value, images = images, viewer = viewer, dsNode = dsNode)
	}
  }
}