package matt.nn.deephy.gui.viewer

import javafx.beans.property.DoubleProperty
import javafx.scene.control.ContentDisplay
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import matt.file.CborFile
import matt.hurricanefx.backgroundColor
import matt.hurricanefx.eye.prop.objectBindingN
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.titled.TitledPaneWrapper
import matt.model.tostringbuilder.toStringBuilder
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
import matt.obs.bind.deepBindingIgnoringFutureNullOuterChanges
import matt.obs.bindings.bool.not
import matt.obs.prop.VarProp
import matt.obs.prop.withChangeListener
import matt.obs.prop.withNonNullUpdatesFrom
import matt.obs.prop.withUpdatesFrom

class DatasetViewer(initialFile: CborFile? = null, val outerBox: DSetViewsVBox): TitledPaneWrapper() {

  val model by lazy { outerBox.model }

  val siblings by lazy { outerBox.children.filtered { it != this } }

  override fun toString() = toStringBuilder("current file" to fileProp.value?.fname)

  val fileProp: VarProp<CborFile?> = VarProp(initialFile).withChangeListener {
	outerBox.save()
  }
  private val dataBinding = fileProp.binding { f ->
	f?.run {
	  loadCbor<Test>().also {
		(it as? Loaded<Test>)?.data?.model = model
	  }
	}
  }

  val boundToDSet by lazy {
	outerBox.bound.binding {
	  if (it != this@DatasetViewer) it else null
	}
  }
  val isBoundToDSet by lazy { boundToDSet.notNull }
  val isUnboundToDSet by lazy { isBoundToDSet.not() }


  private val boundView by lazy { boundToDSet.deepBindingIgnoringFutureNullOuterChanges { it?.view } }
  val view: VarProp<DatasetNodeView> = VarProp(
	boundView.value ?: ByNeuron
  ).withNonNullUpdatesFrom(boundView)

  private val boundLayer by lazy { boundToDSet.deepBindingIgnoringFutureNullOuterChanges { it?.layerSelection } }
  private val boundLayerConversion by lazy {
	boundLayer.binding(
	  dataBinding /*TODO: remove this dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data*/
	) { layer ->

	  model.resolvedLayers.firstOrNull { it.layerID == layer?.layerID }
	}
  }

  val layerSelection: VarProp<ResolvedLayer?> = VarProp(
	boundLayer.value
  ).withNonNullUpdatesFrom(boundLayerConversion)

  private val boundNeuron = boundToDSet.deepBindingIgnoringFutureNullOuterChanges {
	it?.neuronSelection
  }

  private fun ResolvedLayer.neuronThatMatches(n: ResolvedNeuron?) = neurons.firstOrNull {
	it.index == n?.index
  }

  private val boundNeuronConversion = boundNeuron.binding(
	dataBinding, /*TODO: remove this dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data*/
	layerSelection
  ) { neuron ->
	layerSelection.value?.neuronThatMatches(neuron)
  }


  val neuronSelection: VarProp<ResolvedNeuron?> = VarProp<ResolvedNeuron?>(
	boundNeuronConversion.value
  ).apply {
	withNonNullUpdatesFrom(boundNeuronConversion)
	layerSelection.onChange { l ->
	  if (!isBoundToDSet.value) value = l?.neuronThatMatches(value)
	}
  }
  val imageSelection = VarProp<ResolvedDeephyImage?>(null)
  private val topNeuronsFromMyImage = imageSelection.binding(dataBinding) {
	it?.topNeurons()
  }
  private val boundTopNeurons = boundToDSet.deepBindingIgnoringFutureNullOuterChanges {
	it?.topNeurons
  }
  private val boundTopNeuronConversion = boundTopNeurons.binding(dataBinding) { ns->
	ns?.map { n ->
	  model.neurons.first { it.neuron == n.neuron }
	}
  }


  val topNeurons: VarProp<List<ResolvedNeuronLike>?> =
	VarProp<List<ResolvedNeuronLike>?>(boundTopNeuronConversion.value).apply {
	  withUpdatesFrom(topNeuronsFromMyImage)
	  withNonNullUpdatesFrom(boundTopNeuronConversion)
	}

  var currentByImageHScroll: DoubleProperty? = null

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
		"bind", group = this@DatasetViewer.outerBox.myToggleGroup, value = this@DatasetViewer
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



