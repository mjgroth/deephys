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
import matt.log.profile.stopwatch
import matt.log.profile.tic
import matt.model.tostringbuilder.toStringBuilder
import matt.nn.deephy.gui.DEEPHY_FONT
import matt.nn.deephy.gui.DSetViewsVBox
import matt.nn.deephy.gui.dataset.DatasetNode
import matt.nn.deephy.gui.dataset.DatasetNodeView
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.load.asyncLoadSwapper
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.DeephyImage
import matt.nn.deephy.model.ResolvedLayer
import matt.nn.deephy.model.ResolvedNeuron
import matt.nn.deephy.model.ResolvedNeuronLike
import matt.nn.deephy.state.DeephyState
import matt.obs.bind.binding
import matt.obs.bind.deepBindingIgnoringFutureNullOuterChanges
import matt.obs.bindings.bool.not
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.prop.BindableProperty
import matt.obs.prop.VarProp
import matt.obs.prop.withChangeListener
import matt.obs.prop.withNonNullUpdatesFrom
import matt.obs.prop.withUpdatesFromWhen

class DatasetViewer(initialFile: CborFile? = null, val outerBox: DSetViewsVBox): TitledPaneWrapper() {

  val model by lazy { outerBox.model }

  val siblings by lazy { outerBox.children.filtered { it != this } }

  override fun toString() = toStringBuilder("current file" to file.value?.fname)

  val file: VarProp<CborFile?> = VarProp(initialFile).withChangeListener {
	outerBox.save()
  }
  val testData = file.binding { f ->
	val t = tic(prefix = "dataBinding2")
	t.toc("start")
	f?.run {
	  val loader = TestLoader(f, model)
	  t.toc("got loader")
	  loader.start()
	  t.toc("started loader")
	  loader
	}
  }.apply {
	stopwatch = "dataBinding"
  }

  val boundToDSet by lazy {
	outerBox.bound.binding {
	  if (it != this@DatasetViewer) it else null
	}
  }
  val isBoundToDSet by lazy { boundToDSet.isNotNull }
  val isUnboundToDSet by lazy { isBoundToDSet.not() }


  private val boundView by lazy { boundToDSet.deepBindingIgnoringFutureNullOuterChanges { it?.view } }
  val view: VarProp<DatasetNodeView> = VarProp(
	boundView.value ?: ByNeuron
  ).withNonNullUpdatesFrom(boundView)

  private val boundLayer by lazy { boundToDSet.deepBindingIgnoringFutureNullOuterChanges { it?.layerSelection } }
  private val boundLayerConversion by lazy {
	boundLayer.binding(
	  testData /*TODO: remove this dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data*/
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
	testData, /*TODO: remove this dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data*/
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
  val imageSelection = VarProp<DeephyImage?>(null)
  private val topNeuronsFromMyImage =
	imageSelection.binding(testData, layerSelection, DeephyState.normalizeTopNeuronActivations) { im ->
	  layerSelection.value?.let { im?.topNeurons(it) }

	}
  private val boundTopNeurons = boundToDSet.deepBindingIgnoringFutureNullOuterChanges {
	it?.topNeurons
  }
  private val boundTopNeuronConversion = boundTopNeurons.binding(testData) { ns ->
	ns?.map { n ->
	  model.neurons.first { it.neuron == n.neuron }
	}
  }


  val topNeurons: VarProp<List<ResolvedNeuronLike>?> =
	VarProp<List<ResolvedNeuronLike>?>(boundTopNeuronConversion.value).apply {
	  withUpdatesFromWhen(topNeuronsFromMyImage) { !isBoundToDSet.value }
	  withNonNullUpdatesFrom(boundTopNeuronConversion)
	}

  var currentByImageHScroll: DoubleProperty? = null

  val history = basicMutableObservableListOf<TestViewerAction>()
  val historyIndex = BindableProperty(-1)

  init {
	contentDisplay = ContentDisplay.LEFT
	isExpanded = true
	titleProperty.bind(file.binding { it?.nameWithoutExtension })
	graphic = hbox<NodeWrapper> {
	  button("remove test") {
		font = DEEPHY_FONT
		tooltip("remove this test viewer") {
		  font = DEEPHY_FONT
		}
		setOnAction {
		  if (this@DatasetViewer.outerBox.bound.value == this@DatasetViewer) {
			this@DatasetViewer.outerBox.bound.value = null
		  }
		  this@DatasetViewer.removeFromParent()
		  this@DatasetViewer.outerBox.save()
		}
	  }
	  button("select test") {
		font = DEEPHY_FONT
		tooltip("choose test file") {
		  font = DEEPHY_FONT
		}
		setOnAction {
		  val f = FileChooser().apply {
			title = "choose test data"
			this.extensionFilters.setAll(ExtensionFilter("tests", "*.test"))
		  }.showOpenDialog(stage)

		  if (f != null) {
			stopwatch("set fileProp") {
			  this@DatasetViewer.file.value = CborFile(f.path)
			}
		  }
		}
	  }


	  button("back") {
		enableProperty.bind(
		  this@DatasetViewer.history.binding(
			this@DatasetViewer.historyIndex, this@DatasetViewer.view, this@DatasetViewer.boundToDSet
		  ) {
			this@DatasetViewer.isUnboundToDSet.value && this@DatasetViewer.view.value == ByImage && it.isNotEmpty() && this@DatasetViewer.historyIndex.value > 0
		  })
		setOnAction {
		  this@DatasetViewer.historyIndex.value -= 1
		  val action = this@DatasetViewer.history[this@DatasetViewer.historyIndex.value]
		  this@DatasetViewer.imageSelection.value = (action as SelectImage).image
		}

	  }
	  button("forward") {
		enableProperty.bind(
		  this@DatasetViewer.history.binding(
			this@DatasetViewer.historyIndex, this@DatasetViewer.view, this@DatasetViewer.boundToDSet
		  ) {
			this@DatasetViewer.isUnboundToDSet.value && this@DatasetViewer.view.value == ByImage && it.isNotEmpty() && this@DatasetViewer.historyIndex.value < it.size - 1
		  })
		setOnAction {
		  this@DatasetViewer.historyIndex.value += 1
		  val action = this@DatasetViewer.history[this@DatasetViewer.historyIndex.value]
		  this@DatasetViewer.imageSelection.value = (action as SelectImage).image
		}
	  }
	  togglebutton(
		"bind", group = this@DatasetViewer.outerBox.myToggleGroup, value = this@DatasetViewer
	  ) {
		font = DEEPHY_FONT
		backgroundProperty.bind(selectedProperty.objectBindingN {
		  if (it == true) backgroundColor(Color.YELLOW) else null
		})
	  }
	}
	content = asyncLoadSwapper(testData, nullMessage = "select a test to view it") {
	  DatasetNode(this, this@DatasetViewer)
	}.node
  }
}


sealed interface TestViewerAction
class SelectImage(val image: DeephyImage): TestViewerAction