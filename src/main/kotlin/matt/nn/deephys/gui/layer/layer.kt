package matt.nn.deephys.gui.layer

import javafx.scene.paint.Color
import matt.fx.control.wrapper.control.spinner.spinner
import matt.fx.graphics.fxthread.runLater
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.fx.graphics.style.border.FXBorder
import matt.hurricanefx.eye.wrapper.obs.obsval.prop.toNullableProp
import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.model.op.convert.StringConverter
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.neuron.NeuronView
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.obs.bind.binding
import matt.obs.bindings.bool.ObsB
import matt.obs.col.olist.toBasicObservableList
import matt.prim.str.isInt

class LayerView(
  layer: ResolvedLayer,
  testLoader: TestLoader,
  viewer: DatasetViewer
): VBoxWrapperImpl<RegionWrapper<*>>() {
  init {
	val neurons = layer.neurons.map { it.interTest }
	var badText: ObsB? = null
	val neuronSpinner = spinner(
	  items = neurons.toBasicObservableList(),
	  editable = true,
	  //	  property = viewer.neuronSelection,
	  enableScroll = false
	) {
	  valueFactory.converter = object: StringConverter<InterTestNeuron> {
		override fun toString(t: InterTestNeuron): String {
		  return "${t.index}"
		}

		override fun fromString(s: String): InterTestNeuron {
		  return s.toIntOrNull()?.let { i -> neurons.firstOrNull { it.index == i } } ?: neurons.first()
		  //		  val i = string.toIntOrNull()
		  //		  if (i != null) {
		  //		  return neurons.firstOrNull { it.index == i } ?: neurons.first()
		  //		  		  }
		  //		  return null
		}
	  }
	  /*try to force gui update so first one converts to correct string...*/
	  valueFactory.increment(1)
	  valueFactory.decrement(1)

	  //	  valueProperty.onChange {
	  //		println("v:$it")
	  //	  }
	  editor.textProperty.onChange {
		val oldSelection = node.editor.selection
		runLater {
		  node.commitValue()
		  runLater {
			node.editor.selectRange(oldSelection.start + 1, oldSelection.end + 1)
		  }
		}
	  }


	  badText = node.editor.textProperty().toNullableProp().binding {
		it == null || !it.isInt() || it.toInt() !in neurons.indices
	  }

	  badText!!.onChange {
		border = if (it) FXBorder.solid(Color.RED, 10.0)
		else null
	  }



	  valueFactory.value = viewer.neuronSelection.value ?: neurons[0]
	  val rBlocker = RecursionBlocker()
	  viewer.neuronSelection.onChange {
		rBlocker {
		  valueFactory.value = it ?: neurons[0]
		}
	  }
	  valueProperty.onChange {
		rBlocker {
		  viewer.neuronSelection.value = it
		}
	  }

	}
	/*val neuronCB = choicebox(property = viewer.neuronSelection, values = layer.neurons.map { it.interTest }) {
	  converter = toStringConverter<InterTestNeuron?> { "neuron ${it?.index}" }.toFXConverter()
	}*/
	hbox<NodeWrapper> {
	  deephyText("neuron: ")
	  +neuronSpinner
	  deephyText("please input valid integer neuron index between 0 and ${neurons.size}") {
		visibleAndManagedProp.bind(badText!!)
	  }
	  /*+neuronCB*/
	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull)
	}
	/*NEVER NULL*/
	swapper(neuronSpinner.valueProperty, nullMessage = "select a neuron") {
	  NeuronView(this, testLoader = testLoader, viewer = viewer)
	}
  }
}