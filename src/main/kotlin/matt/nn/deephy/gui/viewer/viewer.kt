package matt.nn.deephy.gui.viewer

import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.scene.control.ContentDisplay
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import matt.file.CborFile
import matt.hurricanefx.async.bindLater
import matt.hurricanefx.backgroundColor
import matt.hurricanefx.eye.lang.Prop
import matt.hurricanefx.eye.lang.PropN
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.mtofx.toFXProp
import matt.hurricanefx.eye.prop.objectBindingN
import matt.hurricanefx.eye.prop.stringBindingN
import matt.hurricanefx.eye.wrapper.obs.obsval.toNullableROProp
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.titled.TitledPaneWrapper
import matt.hurricanefx.wrapper.text.TextWrapper
import matt.klib.lang.toStringBuilder
import matt.nn.deephy.gui.DSetViewsVBox
import matt.nn.deephy.gui.dataset.DatasetNode
import matt.nn.deephy.gui.dataset.DatasetNodeView
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.model.FileNotFound
import matt.nn.deephy.model.ParseError
import matt.nn.deephy.model.ResolvedDeephyImage
import matt.nn.deephy.model.ResolvedLayer
import matt.nn.deephy.model.ResolvedNeuron
import matt.nn.deephy.model.ResolvedNeuronLike
import matt.nn.deephy.model.Test
import matt.obs.prop.BindableProperty

class DatasetViewer(initialFile: CborFile? = null, val outerBox: DSetViewsVBox): TitledPaneWrapper() {

  val model get() = outerBox.model

  val siblings get() = outerBox.children.filter { it != this }

  override fun toString() = toStringBuilder("current file" to fileProp.value?.fname)

  val fileProp: Prop<CborFile?> = Prop(initialFile).apply {
	onChange {
	  outerBox.save()
	}
  }
  private val dataBinding = fileProp.objectBindingN {
	if (it != null && it.doesNotExist) FileNotFound
	else {
	  it?.let {
		Cbor.decodeFromByteArray<Test>(it.readBytes()).also {
		  it.model = model
		}
	  }
	}
  }.toNullableROProp()

  val boundTo: ObjectProperty<DatasetViewer?> = PropN<DatasetViewer?>().apply {
	bind(outerBox.bound.objectBindingN {
	  if (it != this@DatasetViewer) it else null
	})
  }
  val isBound get() = boundTo.value != null

  val view: BindableProperty<DatasetNodeView> = BindableProperty(
	boundTo.value?.view?.value ?: ByNeuron
  ).apply {
	this@DatasetViewer.boundTo.onChange {
	  if (it != null) bindLater(it.view)
	  else unbind()
	}
  }

  val layerSelection: ObjectProperty<ResolvedLayer> = Prop<ResolvedLayer>().apply {
	boundTo.onChange { b ->
	  if (b != null) {
		bindLater(
		  b.layerSelection.objectBindingN(dataBinding.toFXProp()) { layer ->
			model.resolvedLayers.firstOrNull { it.layerID == layer?.layerID }
		  }
		)
	  } else unbind()
	}
	dataBinding.onChange {
	  if (!isBound) {
		value = null
	  }
	}
  }
  val neuronSelection: ObjectProperty<ResolvedNeuron> = Prop<ResolvedNeuron>().apply {

	boundTo.onChange { b ->
	  if (b != null) {
		bindLater(
		  b.neuronSelection.objectBindingN(dataBinding.toFXProp(), layerSelection) { n ->
			layerSelection.value?.neurons?.firstOrNull { it.index == n?.index }
		  }
		)
	  } else unbind()
	}

	layerSelection.onChange { l ->
	  if (!isBound) value = l?.neurons?.firstOrNull { it.index == value?.index }
	}

	dataBinding.onChange {
	  if (!isBound) value = null
	}
  }
  val imageSelection = Prop<ResolvedDeephyImage>()


  val topNeurons: ObjectProperty<List<ResolvedNeuronLike>?> = PropN<List<ResolvedNeuronLike>?>().apply {
	if (!isBound) {
	  bindLater(imageSelection.objectBindingN(dataBinding.toFXProp()) { it?.topNeurons() })
	}
	boundTo.onChange { b ->
	  if (b != null) {
		bindLater(b.topNeurons.objectBindingN(dataBinding.toFXProp()) { neurons ->
		  neurons?.let { ns ->
			ns.mapNotNull { n ->
			  model.neurons.first { it.neuron == n.neuron }
			}
		  }
		})

	  } else {
		unbind()
		bindLater(imageSelection.objectBindingN { it?.topNeurons() })
	  }
	}
  }

  var currentByImageHScroll: DoubleProperty? = null
  //
  //
  //
  //  fun reBindByImageHScrolls() {
  //	currentByImageHScrollProp.value?.go { p ->
  //
  //
  //
  //	  boundTo.onChange { b ->
  //		if (b != null) {
  //		  bindLater(b.topNeurons.objectBindingN(dataBinding.toFXProp()) { neurons ->
  //			neurons?.let { ns ->
  //			  ns.mapNotNull { n ->
  //				(dataBinding.value as? Dataset)?.neurons?.first {
  //				  it.layer.layerID == n.layer.layerID
  //					  && it.index == n.index
  //				}
  //			  }
  //			}
  //		  })
  //
  //		} else {
  //		  unbind()
  //		  bindLater(imageSelection.objectBindingN { it?.topNeurons() })
  //		}
  //	  }
  //	}
  //  }


  init {
	contentDisplay = ContentDisplay.LEFT
	isExpanded = true
	titleProperty.bind(fileProp.stringBindingN { it?.nameWithoutExtension })
	graphic = hbox<NodeWrapper> {
	  button("remove dataset") {
		tooltip("remove this dataset viewer")
		setOnAction {
		  if (this@DatasetViewer.outerBox.bound.value == this@DatasetViewer) {
			this@DatasetViewer.outerBox.bound.value = null
		  }
		  this@DatasetViewer.removeFromParent()
		  this@DatasetViewer.outerBox.save()
		}
	  }
	  button("select test") {
		tooltip("choose test file")
		setOnAction {
		  val f = FileChooser().apply {
			title = "choose test data"
			this.extensionFilters.setAll(ExtensionFilter("tests", "*.test"))
		  }.showOpenDialog(stage)

		  if (f != null) {
			this@DatasetViewer.fileProp.value = CborFile(f.path)
		  }
		}
	  }
	  togglebutton("bind", group = this@DatasetViewer.outerBox.myToggleGroup, value = this@DatasetViewer) {
		backgroundProperty.bind(selectedProperty.objectBindingN {
		  if (it == true) backgroundColor(Color.YELLOW) else null
		})
	  }
	}
	content = swapper(dataBinding, nullMessage = "select a dataset to view it") {
	  when (this) {
		is FileNotFound -> TextWrapper("${fileProp.value} not found")
		is ParseError   -> TextWrapper("parse error")
		is Test         -> DatasetNode(this, this@DatasetViewer)
	  }
	}.node
  }
}

