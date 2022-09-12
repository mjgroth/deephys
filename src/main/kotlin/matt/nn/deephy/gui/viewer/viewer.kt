package matt.nn.deephy.gui.viewer

import javafx.beans.property.DoubleProperty
import javafx.scene.control.ContentDisplay
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import matt.file.CborFile
import matt.hurricanefx.async.bindLater
import matt.hurricanefx.backgroundColor
import matt.hurricanefx.eye.prop.objectBindingN
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.titled.TitledPaneWrapper
import matt.lang.toStringBuilder
import matt.nn.deephy.gui.DSetViewsVBox
import matt.nn.deephy.gui.dataset.DatasetNode
import matt.nn.deephy.gui.dataset.DatasetNodeView
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.model.Loaded
import matt.nn.deephy.model.ResolvedDeephyImage
import matt.nn.deephy.model.ResolvedLayer
import matt.nn.deephy.model.ResolvedNeuron
import matt.nn.deephy.model.ResolvedNeuronLike
import matt.nn.deephy.model.Test
import matt.nn.deephy.model.loadCbor
import matt.nn.deephy.model.loadSwapper
import matt.obs.bind.binding
import matt.obs.prop.BindableProperty
import matt.obs.prop.VarProp

class DatasetViewer(initialFile: CborFile? = null, val outerBox: DSetViewsVBox): TitledPaneWrapper() {

  val model get() = outerBox.model

  val siblings get() = outerBox.children.filter { it != this }

  override fun toString() = toStringBuilder("current file" to fileProp.value?.fname)

  val fileProp: VarProp<CborFile?> = VarProp(initialFile).apply {
	onChange {
	  outerBox.save()
	}
  }
  private val dataBinding = fileProp.binding { f ->
	f?.run {
	  loadCbor<Test>().also {
		(it as? Loaded<Test>)?.data?.model = model
	  }
	}
  }

  val boundToDSet: BindableProperty<DatasetViewer?> = BindableProperty<DatasetViewer?>(null).apply {
	bind(outerBox.bound.binding {
	  if (it != this@DatasetViewer) it else null
	})
  }
  val isBound get() = boundToDSet.value != null

  val view: BindableProperty<DatasetNodeView> = BindableProperty(
	boundToDSet.value?.view?.value ?: ByNeuron
  ).apply {
	this@DatasetViewer.boundToDSet.onChange {
	  if (it != null) bindLater(it.view)
	  else unbind()
	}
  }

  val layerSelection: BindableProperty<ResolvedLayer?> = BindableProperty<ResolvedLayer?>(null).apply {
	this@DatasetViewer.boundToDSet.onChange { b ->
	  if (b != null) {
		bindLater(
		  b.layerSelection.binding(dataBinding) { layer ->
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
  val neuronSelection: BindableProperty<ResolvedNeuron?> = BindableProperty<ResolvedNeuron?>(null).apply {

	boundToDSet.onChange { b ->
	  if (b != null) {
		bindLater(
		  b.neuronSelection.binding(dataBinding, layerSelection) { n ->
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
  val imageSelection = BindableProperty<ResolvedDeephyImage?>(null)


  val topNeurons: BindableProperty<List<ResolvedNeuronLike>?> =
	BindableProperty<List<ResolvedNeuronLike>?>(null).apply {
	  if (!isBound) {
		bindLater(imageSelection.binding(dataBinding) { it?.topNeurons() })
	  }
	  boundToDSet.onChange { b ->
		if (b != null) {
		  bindLater(b.topNeurons.binding(dataBinding) { neurons ->
			neurons?.let { ns ->
			  ns.mapNotNull { n ->
				model.neurons.first { it.neuron == n.neuron }
			  }
			}
		  })

		} else {
		  unbind()
		  bindLater(imageSelection.binding { it?.topNeurons() })
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
	titleProperty.bind(fileProp.binding { it?.nameWithoutExtension })
	graphic = hbox<NodeWrapper> {
	  button("remove test") {
		tooltip("remove this test viewer")
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
	  togglebutton(
		"bind", group = this@DatasetViewer.outerBox.myToggleGroup,
		value = this@DatasetViewer
	  ) {
		backgroundProperty.bind(selectedProperty.objectBindingN {
		  if (it == true) backgroundColor(Color.YELLOW) else null
		})
	  }
	}
	content = loadSwapper(dataBinding, nullMessage = "select a test to view it") {
	  DatasetNode(this, this@DatasetViewer)
	}.node
  }
}



