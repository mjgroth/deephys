package matt.nn.deephys.gui.layer

import javafx.scene.paint.Color
import matt.fx.control.wrapper.control.spinner.spinner
import matt.fx.graphics.style.border.FXBorder
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperNeverNull
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.nn.deephys.gui.global.deephysLabeledControl
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.neuron.NeuronView
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
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
  viewer: DatasetViewer,
  override val settings: DeephysSettingsController
): VBoxWrapperImpl<RegionWrapper<*>>(), DeephysNode {
  init {

	val memSafeSettings = settings

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

	  val neuron = viewer.neuronSelection.value?.takeIf { it.layer == interLayer } ?: firstNeuron
	  valueFactory!!.value = neuron
	  viewer.neuronSelection v neuron

	  bindBidirectional(
		viewer.neuronSelection,
		default = firstNeuron,
		acceptIf = {
		  it == null || it.layer == interLayer
		}
	  )
	}


	deephysLabeledControl("Neuron", neuronSpinner) {
	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull)
	}
	deephysText("please input valid integer neuron index between 0 and ${neurons.size}") {
	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull.and(badText!!))
	}
	swapperNeverNull(neuronSpinner.valueProperty) {
	  NeuronView(
		this,
		testLoader = testLoader,
		viewer = viewer,
		showActivationRatio = true,
		layoutForList = false,
		settings = memSafeSettings,
		showTopCats = true
	  )
	}
  }
}