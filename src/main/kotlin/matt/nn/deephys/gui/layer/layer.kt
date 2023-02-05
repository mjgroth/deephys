package matt.nn.deephys.gui.layer

import javafx.scene.paint.Color
import matt.fx.control.wrapper.control.spinner.spinner
import matt.fx.graphics.style.border.FXBorder
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperNeverNull
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.deephysLabeledControl
import matt.nn.deephys.gui.neuron.NeuronView
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.obs.bind.binding
import matt.obs.bindings.bool.ObsB
import matt.obs.bindings.bool.and
import matt.obs.col.olist.toBasicObservableList
import matt.prim.str.isInt

class LayerView(
  layer: ResolvedLayer,
  testLoader: TypedTestLike<*>,
  viewer: DatasetViewer
): VBoxWrapperImpl<RegionWrapper<*>>() {
  init {


	val interLayer = layer.interTest
	val neurons = layer.neurons.map { it.interTest }
	var badText: ObsB? = null
	val firstNeuron = neurons[0]
	val neuronSpinner = spinner(
	  items = neurons.toBasicObservableList(),
	  editable = true,
	  enableScroll = false,
	  converter = InterTestNeuron.stringConverterThatFallsBackToFirst(neurons = neurons)
	) {

	  autoCommitOnType()

	  valueFactory!!.wrapAround = true

	  badText = textProperty.binding {
		it == null || !it.isInt() || it.toInt() !in neurons.indices
	  }

	  badText!!.onChange {
		border = if (it) FXBorder.solid(Color.RED, 10.0)
		else null
	  }


	  val fact = valueFactory!!
	  val neuron = viewer.neuronSelection.value?.takeIf { it.layer == interLayer } ?: firstNeuron
	  fact.value = neuron
	  viewer.neuronSelection v neuron
	  val rBlocker = RecursionBlocker()
	  viewer.neuronSelection.onChangeWithWeak(fact) { deRefedFact, newNeuron ->
		if (newNeuron == null || newNeuron.layer == interLayer) {
		  rBlocker {
			deRefedFact.value = newNeuron ?: firstNeuron
		  }
		}
	  }
	  valueProperty.onChangeWithWeak(viewer) { deRefedViewer, newValue ->
		rBlocker {
		  deRefedViewer.neuronSelection.value = newValue
		}
	  }

	}


	deephysLabeledControl("Neuron", neuronSpinner) {
	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull)
	}
	deephyText("please input valid integer neuron index between 0 and ${neurons.size}") {
	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull.and(badText!!))
	}
	swapperNeverNull(neuronSpinner.valueProperty) {
	  NeuronView(this, testLoader = testLoader, viewer = viewer, showActivationRatio = true, layoutForList = false)
	}
  }
}