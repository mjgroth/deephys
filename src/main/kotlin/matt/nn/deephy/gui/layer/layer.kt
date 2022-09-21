package matt.nn.deephy.gui.layer

import matt.hurricanefx.eye.converter.toFXConverter
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.model.convert.toStringConverter
import matt.nn.deephy.gui.DEEPHY_FONT
import matt.nn.deephy.gui.neuron.NeuronView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.ResolvedLayer
import matt.nn.deephy.model.ResolvedNeuron

class LayerView(
  layer: ResolvedLayer,
  testLoader: TestLoader,
  viewer: DatasetViewer
): VBoxWrapper<RegionWrapper<*>>() {
  init {
	val neuronCB = choicebox(property = viewer.neuronSelection, values = layer.neurons) {
	  converter = toStringConverter<ResolvedNeuron?> { "neuron ${it?.index}" }.toFXConverter()
	}
	hbox<NodeWrapper> {
	  text("neuron: ") {
		font = DEEPHY_FONT
	  }
	  +neuronCB
	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull)
	}
	swapper(neuronCB.valueProperty, nullMessage = "select a neuron") {
	  NeuronView(this, testLoader = testLoader, viewer = viewer)
	}
  }
}