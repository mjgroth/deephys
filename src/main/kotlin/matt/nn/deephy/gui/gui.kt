package matt.nn.deephy.gui

import javafx.scene.control.ContentDisplay
import javafx.scene.control.ToggleGroup
import javafx.scene.paint.Color.YELLOW
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import matt.file.CborFile
import matt.file.construct.toMFile
import matt.file.toSFile
import matt.hurricanefx.backgroundColor
import matt.hurricanefx.eye.lang.BProp
import matt.hurricanefx.eye.lang.Prop
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.mtofx.toFXProp
import matt.hurricanefx.eye.prop.objectBindingN
import matt.hurricanefx.eye.prop.stringBindingN
import matt.hurricanefx.eye.wrapper.obs.obsval.toNullableROProp
import matt.hurricanefx.tornadofx.control.selectedValueProperty
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.titled.TitledPaneWrapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.text.TextWrapper
import matt.nn.deephy.gui.dataset.DatasetNode
import matt.nn.deephy.model.Dataset
import matt.nn.deephy.model.DeephyImage
import matt.nn.deephy.model.FileNotFound
import matt.nn.deephy.model.Layer
import matt.nn.deephy.model.Neuron
import matt.nn.deephy.model.ParseError
import matt.nn.deephy.state.DeephyState
import matt.stream.message.FileList


class DSetViewsVBox: VBoxWrapper<DatasetViewer>() {
  operator fun plusAssign(file: CborFile) {
	this += DatasetViewer(file, this)
  }

  fun save() {
	DeephyState.datasets.value = FileList(children.mapNotNull { it.fileProp.value?.toSFile() })
  }

  private val layerSelection = Prop<String>()
  private val neuronSelection = Prop<Int>()

  val myToggleGroup = ToggleGroup().apply {

	fun refreshBind(selected: DatasetViewer?) {
	  layerSelection.unbind()
	  neuronSelection.unbind()

	  if (selected == null) {
		children.forEach {
		  it.layerSelection.unbind()
		  it.neuronSelection.unbind()
		  it.bound.value = false
		}
	  } else {
		selected.apply {
		  layerSelection.unbind()
		  neuronSelection.unbind()
		  bound.value = false
		}

		val notSelected = children.filter { it != selected }
		notSelected.forEach {
		  val ns = it
		  val dataFXProp = ns.dataBinding.toFXProp()
		  ns.layerSelection.bind(layerSelection.objectBindingN(dataFXProp) { layerID ->
			(ns.dataBinding.value as? Dataset)?.layers?.firstOrNull { it.layerID == layerID }
		  })
		  ns.neuronSelection.bind(neuronSelection.objectBindingN(dataFXProp, ns.layerSelection) { index ->
			ns.layerSelection.value?.neurons?.withIndex()?.firstOrNull { it.index == index }
		  })
		  ns.bound.value = true
		}

		layerSelection.bind(selected.layerSelection.stringBindingN { it?.layerID })
		neuronSelection.bind(selected.neuronSelection.objectBindingN { it?.index })

	  }


	}

	val selProp = selectedValueProperty<DatasetViewer>()
	selProp.onChange { selected ->
	  refreshBind(selected)
	}
	children.onChange {
	  if (selProp.value !in children) {
		refreshBind(null)
	  } else {
		refreshBind(selProp.value)
	  }

	}

  }

}

class DatasetViewer(initialFile: CborFile? = null, val outerBox: DSetViewsVBox): TitledPaneWrapper() {
  //  val siblings get() = outerBox.children.filter { it != this }
  val fileProp: Prop<CborFile?> = Prop(initialFile).apply {
	onChange {
	  outerBox.save()
	}
  }
  val dataBinding = fileProp.objectBindingN {
	if (it != null && it.doesNotExist) FileNotFound
	else {
	  it?.let {
		Cbor.decodeFromByteArray<Dataset>(it.readBytes())
	  }
	}
  }.toNullableROProp()

  val bound = BProp(false)
  val layerSelection = Prop<Layer>()
  val neuronSelection = Prop<IndexedValue<Neuron>>().apply {
	layerSelection.onChange {
	  if (!bound.value) {
		value = it?.neurons?.withIndex()?.firstOrNull { it.index == value?.index }
	  }
	}
  }
  val imageSelection = Prop<DeephyImage>()


  init {
	contentDisplay = ContentDisplay.LEFT
	isExpanded = true
	titleProperty.bind(fileProp.stringBindingN { it?.nameWithoutExtension })
	graphic = hbox<NodeWrapper> {
	  button("remove dataset") {
		tooltip("remove this dataset viewer")
		setOnAction {
		  this@DatasetViewer.removeFromParent()
		  this@DatasetViewer.outerBox.save()
		}
	  }
	  button("select dataset") {
		tooltip("choose dataset file")
		setOnAction {
		  val f = FileChooser().apply {
			title = "choose data folder"
			this.extensionFilters.setAll(ExtensionFilter("cbor", "*.cbor"))
		  }.showOpenDialog(stage)
		  if (f != null) {
			this@DatasetViewer.fileProp.value = f.toMFile() as CborFile
		  }
		}
	  }
	  togglebutton("bind", group = this@DatasetViewer.outerBox.myToggleGroup, value = this@DatasetViewer) {
		backgroundProperty.bind(selectedProperty.objectBindingN {
		  if (it == true) backgroundColor(YELLOW) else null
		})
	  }
	}
	content = swapper(dataBinding, nullMessage = "select a dataset to view it") {
	  when (this) {
		is FileNotFound -> TextWrapper("${fileProp.value} not found")
		is ParseError   -> TextWrapper("parse error")
		is Dataset      -> {
		  if (!bound.value) {
			layerSelection.value = null
			neuronSelection.value = null
		  }
		  DatasetNode(this, this@DatasetViewer)
		}
	  }
	}.node
  }
}

