package matt.nn.deephys.gui.viewer

import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import matt.async.thread.daemon
import matt.collect.itr.filterNotNull
import matt.collect.set.contents.contentsOf
import matt.collect.weak.lazyWeakMap
import matt.file.CborFile
import matt.fx.control.inter.contentDisplay
import matt.fx.control.inter.graphic
import matt.fx.control.wrapper.control.ControlWrapper
import matt.fx.control.wrapper.control.button.button
import matt.fx.control.wrapper.progressbar.progressbar
import matt.fx.control.wrapper.titled.TitledPaneWrapper
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.node.enableWhen
import matt.fx.graphics.wrapper.node.visibleAndManagedWhen
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.hurricanefx.eye.prop.lastIndexProperty
import matt.hurricanefx.eye.prop.sizeProperty
import matt.lang.weak.MyWeakRef
import matt.log.profile.stopwatch.stopwatch
import matt.log.profile.stopwatch.tic
import matt.log.warn.warn
import matt.model.obj.tostringbuilder.toStringBuilder
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.gui.dataset.DatasetNode
import matt.nn.deephys.gui.dataset.DatasetNodeView
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.viewer.tutorial.bind.BindTutorial
import matt.nn.deephys.load.asyncLoadSwapper
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.load.test.dtype.topNeurons
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.ResolvedNeuron
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.state.DeephySettings
import matt.obs.bind.MyBinding
import matt.obs.bind.binding
import matt.obs.bind.coalesceNull
import matt.obs.bind.deepBinding
import matt.obs.bind.deepBindingIgnoringFutureNullOuterChanges
import matt.obs.bind.weakBinding
import matt.obs.bindings.bool.and
import matt.obs.bindings.bool.not
import matt.obs.bindings.comp.gt
import matt.obs.bindings.comp.lt
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal
import matt.obs.prop.VarProp
import matt.obs.prop.toVarProp
import matt.obs.prop.withChangeListener
import matt.obs.prop.withNonNullUpdatesFrom
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class) class DatasetViewer(initialFile: CborFile? = null, val outerBox: DSetViewsVBox):
	TitledPaneWrapper() {

  private val initStopwatch = tic("viewer init", enabled = false)

  val showAsList1 = BindableProperty(false)
  val showAsList2 = BindableProperty(false)

  init {
	initStopwatch.toc(1)
  }

  val model by lazy { outerBox.model }

  val siblings by lazy { outerBox.children.filtered { it != this } }

  override fun toString() = toStringBuilder("current file" to file.value?.fName)

  val file: VarProp<CborFile?> = VarProp(initialFile).withChangeListener {
	outerBox.save()
  }

  val smallImageScale = BindableProperty(DeephySettings.smallImageScale.value).apply {
	bind(DeephySettings.smallImageScale)
  }
  val bigImageScale = BindableProperty(DeephySettings.bigImageScale.value).apply {
	bind(DeephySettings.bigImageScale)
  }
  val normalizeTopNeuronActivations = BindableProperty(DeephySettings.normalizeTopNeuronActivations.value).apply {
	bind(DeephySettings.normalizeTopNeuronActivations)
  }
  val numImagesPerNeuronInByImage = BindableProperty(DeephySettings.numImagesPerNeuronInByImage.value).apply {
	bind(DeephySettings.numImagesPerNeuronInByImage)
  }
  val predictionSigFigs = BindableProperty(DeephySettings.predictionSigFigs.value).apply {
	bind(DeephySettings.predictionSigFigs)
  }
  val showCacheBars = BindableProperty(DeephySettings.showCacheBars.value).apply {
	bind(DeephySettings.showCacheBars)
  }
  val showTutorials = BindableProperty(DeephySettings.showTutorials.value).apply {
	bind(DeephySettings.showTutorials)
  }

  val inD = BindableProperty(outerBox.inD.value).apply {
	bind(outerBox.inD)
  }

  val numViewers = BindableProperty(outerBox.children.size).apply {
	bind(outerBox.children.sizeProperty)
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

  val outerBoundDSet = BindableProperty(outerBox.bound.value).apply {
	bind(outerBox.bound)
  }

  val boundToDSet by lazy {
	outerBoundDSet.binding {
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
  ).withNonNullUpdatesFrom(boundNeuron)

  init {
	initStopwatch.toc(3)
  }

  val neuronSelectionResolved = neuronSelection.binding(
	testData, /*TODO: remove this dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data*/
	layerSelectionResolved
  ) { neuron ->
	layerSelectionResolved.value?.neurons?.firstOrNull { it.index == neuron?.index }
  }


  val imageSelection = VarProp<DeephyImage<*>?>(null)


  private val topNeuronsFromMyImage = run {
	imageSelection.binding(
	  testData, layerSelection, normalizeTopNeuronActivations, inD
	) { im ->
	  layerSelection.value?.let { lay ->
		im?.let { theIm ->
		  testData.value!!.dtype.topNeurons(
			images = contentsOf(theIm),
			layer = lay,
			normalized = normalizeTopNeuronActivations.value,
			test = testData.value!!.preppedTest.await(),
			denomTest = inD.value.takeIf { it != this }?.testData?.value?.preppedTest?.await()
		  )
		}
	  }
	}
  }


  val boundTopNeurons: MyBinding<TopNeurons<*>?> = boundToDSet.deepBinding(
	normalizeTopNeuronActivations,
	inD
  ) {


	it?.topNeurons?.binding(
	  normalizeTopNeuronActivations, inD
	) {
	  it?.let {

		testData.value!!.dtype.topNeurons(
		  images = contentsOf(),
		  layer = it.layer,
		  test = testData.value!!.preppedTest.await(),
		  denomTest = inD.value?.testData?.value?.preppedTest?.await(),
		  normalized = normalizeTopNeuronActivations.value,
		  forcedNeuronIndices = it().map { it.neuron.index }
		)


	  }
	} ?: BindableProperty(null)


  }


  val topNeurons: MyBinding<TopNeurons<*>?> = boundTopNeurons coalesceNull topNeuronsFromMyImage


  init {
	initStopwatch.toc(4)
  }

  val highlightedNeurons = MyBinding(
	view, topNeurons, neuronSelection
  ) {
	when (view.value) {
	  ByNeuron   -> listOf(neuronSelection.value).filterNotNull()
	  ByImage    -> topNeurons.value?.findOrCompute() ?: listOf()
	  ByCategory -> listOf<InterTestNeuron>().apply {
		warn("did not make highlighted neurons from category view work yet")
	  }
	}
  }

  val weakRef = MyWeakRef(this)

  private val boundCategory: ObsVal<CategorySelection?> = boundToDSet
	.deepBindingIgnoringFutureNullOuterChanges(testData) {
	  it?.catSelectionForViewer?.get(weakRef.deref()!!) ?: BindableProperty(null)
	  //	  it?.categorySelection?.binding { cat ->
	  //		testData.value?.let { tst ->
	  //		  cat?.forTest(tst)
	  //		}
	  //	  } ?: BindableProperty(null)
	}


  val categorySelection = VarProp<CategorySelection?>(null).withNonNullUpdatesFrom(boundCategory)
  private val catSelectionForViewer = lazyWeakMap<DatasetViewer, ObsVal<CategorySelection?>> { viewer ->
	categorySelection.weakBinding(viewer) { v, cat ->
	  v.testData.value?.let { tst ->
		cat?.forTest(tst)
	  }
	}
  }


  var currentByImageHScroll: VarProp<Double>? = null

  val history = basicMutableObservableListOf<TestViewerAction>()
  val historyIndex = VarProp(-1)

  init {
	initStopwatch.toc(5)
  }

  private fun appendHistory(historyAction: TestViewerAction) {
	for (i in (historyIndex.value + 1)..<history.size) {
	  history.removeAt(historyIndex.value + 1)
	}
	history.add(historyAction)
	historyIndex.value += 1
  }

  fun navigateTo(neuron: InterTestNeuron, addHistory: Boolean = true) {
	require(!isBoundToDSet.value)
	neuronSelection.value = null
	layerSelection.value = neuron.layer
	neuronSelection.value = neuron
	if (addHistory) appendHistory(SelectNeuron(neuron))
	view.value = ByNeuron
  }


  fun navigateTo(im: DeephyImage<*>, addHistory: Boolean = true) {
	if (isBoundToDSet.value) outerBox.selectViewerToBind(null)
	imageSelection.value = im
	if (addHistory) appendHistory(SelectImage(im))
	view.value = ByImage
  }

  fun navigateTo(category: CategorySelection, addHistory: Boolean = true) {
	if (isBoundToDSet.value) outerBox.selectViewerToBind(null)
	neuronSelection.value = null
	neuronSelection.value = null
	categorySelection.value = category
	if (addHistory) appendHistory(SelectCategory(category))
	view.value = ByCategory
  }

  init {
	initStopwatch.toc(6)
  }

  var bindButton: ControlWrapper? = null
  var oodButton: ControlWrapper? = null

  init {
	initStopwatch.toc(7)
	contentDisplay = ContentDisplay.LEFT
	isExpanded = true    /*titleProperty.bind(file.binding { it?.nameWithoutExtension })*/
	initStopwatch.toc(8)
	graphic = hbox<NodeWrapper> {
	  alignment = Pos.CENTER
	  this@DatasetViewer.initStopwatch.toc(8.1)



	  deephyButton("remove test") {
		veryLazyDeephysTooltip("remove this test viewer")
		setOnAction {
		  this@DatasetViewer.outerBox.removeTest(this@DatasetViewer)
		}
	  }





	  this@DatasetViewer.initStopwatch.toc(8.2)



	  deephyButton("select test") {
		veryLazyDeephysTooltip("choose test file")
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

	  fun redoHistory() {
		val action = this@DatasetViewer.history[this@DatasetViewer.historyIndex.value]
		when (action) {
		  is SelectImage    -> {
			this@DatasetViewer.navigateTo(action.image, addHistory = false)
		  }

		  is SelectCategory -> {
			this@DatasetViewer.navigateTo(action.cat, addHistory = false)
		  }

		  is SelectNeuron   -> {
			this@DatasetViewer.navigateTo(action.neuron, addHistory = false)
		  }
		}
	  }

	  val canUseHistory = this@DatasetViewer.history.binding(
		this@DatasetViewer.historyIndex, this@DatasetViewer.boundToDSet
	  ) {
		this@DatasetViewer.isUnboundToDSet.value && it.isNotEmpty()
	  }

	  button("back") {
		enableWhen {
		  canUseHistory and this@DatasetViewer.historyIndex.gt(0)
		}
		setOnAction {
		  this@DatasetViewer.historyIndex.value -= 1
		  redoHistory()
		}

	  }
	  this@DatasetViewer.initStopwatch.toc(8.4)
	  button("forward") {
		enableWhen {
		  canUseHistory and this@DatasetViewer.historyIndex.lt(this@DatasetViewer.history.lastIndexProperty)
		}
		setOnAction {
		  this@DatasetViewer.historyIndex.value += 1
		  redoHistory()
		}
	  }

	  this@DatasetViewer.bindButton = this@DatasetViewer.outerBox.createBindToggleButton(this, this@DatasetViewer)
	  this@DatasetViewer.oodButton = this@DatasetViewer.outerBox.createInDToggleButton(this, this@DatasetViewer)

	  deephyText(this@DatasetViewer.file.binding { it?.nameWithoutExtension ?: "please select a test" }) {
		titleFont()
	  }
	  progressbar {
		progressProperty.bind(this@DatasetViewer.testData.deepBinding {
		  it?.progress ?: 0.0.toVarProp()
		})
	  }
	  progressbar {
		visibleAndManagedWhen { this@DatasetViewer.showCacheBars }
		style = "-fx-accent: green"
		progressProperty.bind(this@DatasetViewer.testData.deepBinding {
		  it?.cacheProgressPixels ?: 0.0.toVarProp()
		})
	  }
	  progressbar {
		visibleAndManagedWhen { this@DatasetViewer.showCacheBars }
		style = "-fx-accent: yellow"
		progressProperty.bind(this@DatasetViewer.testData.deepBinding {
		  it?.cacheProgressActs ?: 0.0.toVarProp()
		})
	  }
	  deephyText("") {
		fun update(v: TestLoader?) {
		  text = ""
		  if (v != null) {
			daemon {
			  try {
				runBlocking {
				  withTimeout(1.seconds) {
					val dtype = v.preppedTest.await().dtype
					text = "dtype: ${dtype.label}"
				  }
				}
			  } catch (e: TimeoutCancellationException) {
				warn("timeout when getting dtype text!")
			  }

			}

		  }
		}
		update(this@DatasetViewer.testData.value)
		this@DatasetViewer.testData.onChange {
		  update(it)
		}
	  }
	}
	content = v {
	  asyncLoadSwapper(
		this@DatasetViewer.testData,
		nullMessage = "select a test to view it",
		fadeOutDur = DEEPHYS_FADE_DUR,
		fadeInDur = DEEPHYS_FADE_DUR
	  ) {
		DatasetNode(this, this@DatasetViewer)
	  }
	  +BindTutorial(this@DatasetViewer)
	}
  }


}


sealed interface TestViewerAction
class SelectImage(val image: DeephyImage<*>): TestViewerAction
class SelectNeuron(val neuron: InterTestNeuron): TestViewerAction
class SelectCategory(val cat: CategorySelection): TestViewerAction