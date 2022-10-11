package matt.nn.deephys.gui.viewer

import javafx.beans.property.DoubleProperty
import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import matt.collect.itr.filterNotNull
import matt.file.CborFile
import matt.fx.control.inter.contentDisplay
import matt.fx.control.inter.graphic
import matt.fx.control.wrapper.control.button.button
import matt.fx.control.wrapper.progressbar.progressbar
import matt.fx.control.wrapper.titled.TitledPaneWrapper
import matt.fx.graphics.style.backgroundColor
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.hurricanefx.eye.prop.objectBindingN
import matt.lang.go
import matt.log.profile.stopwatch.stopwatch
import matt.log.profile.stopwatch.tic
import matt.log.warn.warn
import matt.model.tostringbuilder.toStringBuilder
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.calc.UniqueContents
import matt.nn.deephys.gui.dataset.DatasetNode
import matt.nn.deephys.gui.dataset.DatasetNodeView
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.deephyToggleButton
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.load.asyncLoadSwapper
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.ResolvedNeuron
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.DeephyImage
import matt.nn.deephys.state.DeephySettings
import matt.obs.bind.MyBinding
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

  private val initStopwatch = tic("viewer init", enabled = false)

  init {
	initStopwatch.toc(1)
  }

  val model by lazy { outerBox.model }

  val siblings by lazy { outerBox.children.filtered { it != this } }

  override fun toString() = toStringBuilder("current file" to file.value?.fName)

  val file: VarProp<CborFile?> = VarProp(initialFile).withChangeListener {
	outerBox.save()
  }
  val testData = file.binding { f ->
	val t = tic(prefix = "dataBinding2", enabled = false)
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

  init {
	initStopwatch.toc(2)
  }

  private val boundView by lazy { boundToDSet.deepBindingIgnoringFutureNullOuterChanges { it?.view } }
  val view: VarProp<DatasetNodeView> = VarProp(
	boundView.value ?: ByNeuron
  ).withNonNullUpdatesFrom(boundView)

  private val boundLayer by lazy { boundToDSet.deepBindingIgnoringFutureNullOuterChanges { it?.layerSelection } }

  val layerSelection: VarProp<InterTestLayer?> = VarProp(
	boundLayer.value
  ).withNonNullUpdatesFrom(boundLayer)

  val layerSelectionResolved = layerSelection.binding(
	testData /*TODO: remove this dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data*/
  ) { layer ->
	model.resolvedLayers.firstOrNull { it.layerID == layer?.layerID }
  }

  private val boundNeuron = boundToDSet.deepBindingIgnoringFutureNullOuterChanges {
	it?.neuronSelection
  }

  private fun ResolvedLayer.neuronThatMatches(n: ResolvedNeuron?) = neurons.firstOrNull {
	it.index == n?.index
  }


  val neuronSelection: VarProp<InterTestNeuron?> = VarProp<InterTestNeuron?>(
	boundNeuron.value
  ).apply {
	withNonNullUpdatesFrom(boundNeuron)
	//	layerSelection.onChange { l ->
	//	  if (!isBoundToDSet.value) value = l?.neuronThatMatches(value)
	//	}
  }

  init {
	initStopwatch.toc(3)
  }

  val neuronSelectionResolved = neuronSelection.binding(
	testData, /*TODO: remove this dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data*/
	layerSelectionResolved
  ) { neuron ->
	layerSelectionResolved.value?.neurons?.firstOrNull { it.index == neuron?.index }
  }


  val imageSelection = VarProp<DeephyImage?>(null)


  private val topNeuronsFromMyImage =
	imageSelection.binding(
	  testData,
	  layerSelection,
	  DeephySettings.normalizeTopNeuronActivations
	) { im ->
	  layerSelection.value?.let { lay ->
		im?.let { TopNeurons(UniqueContents(setOf(it)), lay, DeephySettings.normalizeTopNeuronActivations.value) }
	  }
	}
  private val boundTopNeurons = boundToDSet.deepBindingIgnoringFutureNullOuterChanges {
	it?.topNeurons
  }

  val topNeurons: VarProp<TopNeurons?> =
	VarProp(boundTopNeurons.value).apply {
	  withUpdatesFromWhen(topNeuronsFromMyImage) { !isBoundToDSet.value }
	  withNonNullUpdatesFrom(boundTopNeurons)
	}

  init {
	initStopwatch.toc(4)
  }

  val highlightedNeurons = MyBinding(
	view, topNeurons, neuronSelection
  ) {
	when (view.value) {
	  ByNeuron -> listOf(neuronSelection.value).filterNotNull()
	  ByImage -> topNeurons.value?.findOrCompute() ?: listOf()
	  ByCategory -> listOf<InterTestNeuron>().apply {
		warn("did not make highlighted neurons from category view work yet")
	  }
	}
  }


  val categorySelection = BindableProperty<CategorySelection?>(null)

  var currentByImageHScroll: DoubleProperty? = null

  val history = basicMutableObservableListOf<TestViewerAction>()
  val historyIndex = BindableProperty(-1)

  init {
	initStopwatch.toc(5)
  }

  fun navigateTo(neuron: InterTestNeuron) {
	require(!isBoundToDSet.value)
	neuronSelection.value = null
	layerSelection.value = neuron.layer
	neuronSelection.value = neuron
	view.value = ByNeuron
  }

  fun navigateTo(im: DeephyImage) {
	val t = tic("navigating to image")
	t.toc(1)
	if (isBoundToDSet.value) outerBox.myToggleGroup.selectToggle(null)
	t.toc(2)
	imageSelection.value = im
	t.toc(3)
	for (i in (historyIndex.value + 1) until history.size) {
	  history.removeAt(historyIndex.value + 1)
	}
	t.toc(4)
	history.add(SelectImage(im))
	t.toc(5)
	historyIndex.value += 1
	t.toc(6)
	view.value = ByImage
	t.toc(7)
  }

  fun navigateTo(category: CategorySelection) {
	outerBox.bound.value = null
	neuronSelection.value = null
	neuronSelection.value = null
	categorySelection.value = category
	view.value = ByCategory
  }

  init {
	initStopwatch.toc(6)
  }

  init {
	initStopwatch.toc(7)
	contentDisplay = ContentDisplay.LEFT
	isExpanded = true
	/*titleProperty.bind(file.binding { it?.nameWithoutExtension })*/
	initStopwatch.toc(8)
	graphic = hbox<NodeWrapper> {
	  alignment = Pos.CENTER
	  this@DatasetViewer.initStopwatch.toc(8.1)
	  deephyButton("remove test") {
		this@DatasetViewer.initStopwatch.toc("8.1.1")
		deephyTooltip("remove this test viewer")
		this@DatasetViewer.initStopwatch.toc("8.1.2")
		setOnAction {
		  if (this@DatasetViewer.outerBox.bound.value == this@DatasetViewer) {
			this@DatasetViewer.outerBox.bound.value = null
		  }
		  this@DatasetViewer.removeFromParent()
		  this@DatasetViewer.outerBox.save()
		}
		this@DatasetViewer.initStopwatch.toc("8.1.3")
	  }
	  this@DatasetViewer.initStopwatch.toc(8.2)
	  deephyButton("select test") {
		deephyTooltip("choose test file")
		setOnAction {
		  val f = FileChooser().apply {
			title = "choose test data"
			this.extensionFilters.setAll(ExtensionFilter("tests", "*.test"))
		  }.showOpenDialog(stage?.node)

		  if (f != null) {
			stopwatch("set fileProp") {
			  this@DatasetViewer.file.value = CborFile(f.path)
			}
		  }
		}
	  }
	  this@DatasetViewer.initStopwatch.toc(8.3)

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
	  this@DatasetViewer.initStopwatch.toc(8.4)
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
	  this@DatasetViewer.initStopwatch.toc(8.5)
	  deephyToggleButton(
		"bind", group = this@DatasetViewer.outerBox.myToggleGroup, value = this@DatasetViewer
	  ) {
		backgroundProperty.bind(selectedProperty.objectBindingN {
		  if (it == true) backgroundColor(Color.YELLOW) else null
		})
	  }
	  this@DatasetViewer.initStopwatch.toc(8.6)
	  deephyText(this@DatasetViewer.file.binding { it?.nameWithoutExtension ?: "please select a test" }) {
		titleFont()
	  }
	  progressbar {
		visibleAndManagedProp.bind(progressProperty.neq(1.0))
		this@DatasetViewer.testData.value?.progress?.let {
		  progressProperty.bind(it)
		}
		this@DatasetViewer.testData.onChange {
		  it?.progress?.go {
			progressProperty.bind(it)
		  }
		}
	  }
	}
	this@DatasetViewer.initStopwatch.toc(9)
	content = asyncLoadSwapper(testData, nullMessage = "select a test to view it") {
	  DatasetNode(this, this@DatasetViewer)
	}.node
	this@DatasetViewer.initStopwatch.toc(10)
  }
}


sealed interface TestViewerAction
class SelectImage(val image: DeephyImage): TestViewerAction