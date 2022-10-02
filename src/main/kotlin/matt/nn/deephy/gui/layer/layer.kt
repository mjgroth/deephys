package matt.nn.deephy.gui.layer

import matt.hurricanefx.eye.converter.toFXConverter
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapper
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.model.convert.toStringConverter
import matt.nn.deephy.gui.global.deephyText
import matt.nn.deephy.gui.neuron.NeuronView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.ResolvedLayer
import matt.nn.deephy.model.data.InterTestNeuron

class LayerView(
  layer: ResolvedLayer,
  testLoader: TestLoader,
  viewer: DatasetViewer
): VBoxWrapper<RegionWrapper<*>>() {
  init {
	val neuronCB = choicebox(property = viewer.neuronSelection, values = layer.neurons.map { it.interTest }) {
	  converter = toStringConverter<InterTestNeuron?> { "neuron ${it?.index}" }.toFXConverter()
	}
	hbox<NodeWrapper> {
	  deephyText("neuron: ")
	  +neuronCB
	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull)
	}
	swapper(neuronCB.valueProperty, nullMessage = "select a neuron") {
	  NeuronView(this, testLoader = testLoader, viewer = viewer)
	}
  }
}