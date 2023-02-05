package matt.nn.deephys.gui.viewer

import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import matt.collect.itr.filterNotNull
import matt.collect.set.contents.contentsOf
import matt.collect.weak.lazyWeakMap
import matt.file.CborFile
import matt.fx.control.inter.contentDisplay
import matt.fx.control.inter.graphic
import matt.fx.control.wrapper.control.ControlWrapper
import matt.fx.control.wrapper.progressbar.progressbar
import matt.fx.control.wrapper.titled.TitledPaneWrapper
import matt.fx.graphics.icon.svg.svgToImage2
import matt.fx.graphics.wrapper.imageview.imageview
import matt.fx.graphics.wrapper.node.enableWhen
import matt.fx.graphics.wrapper.node.visibleAndManagedWhen
import matt.fx.graphics.wrapper.pane.hSpacer
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.fx.image.toFXImage
import matt.hurricanefx.eye.prop.lastIndexProperty
import matt.hurricanefx.eye.prop.sizeProperty
import matt.lang.weak.MyWeakRef
import matt.log.profile.stopwatch.stopwatch
import matt.log.profile.stopwatch.tic
import matt.log.warn.warn
import matt.model.obj.tostringbuilder.toStringBuilder
import matt.mstruct.rstruct.resourceStream
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.gui.dataset.DatasetNode
import matt.nn.deephys.gui.dataset.DatasetNodeView
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.DEEPHYS_FONT_MONO
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.deephysInfoSymbol
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.settings.DeephySettings
import matt.nn.deephys.gui.viewer.action.SelectCategory
import matt.nn.deephys.gui.viewer.action.SelectImage
import matt.nn.deephys.gui.viewer.action.SelectNeuron
import matt.nn.deephys.gui.viewer.action.TestViewerAction
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
import matt.prim.str.mybuild.string

class DatasetViewer(initialFile: CborFile? = null, val outerBox: DSetViewsVBox): TitledPaneWrapper() {

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

  //  val normalizeTopNeuronActivations = BindableProperty(DeephySettings.normalizeTopNeuronActivations.value).apply {
  //	bind(DeephySettings.normalizeTopNeuronActivations)
  //  }
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

  val normalizer = BindableProperty(outerBox.normalizer.value).apply {
	bind(outerBox.normalizer)
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

  val neuronSelectionResolved = neuronSelection.binding(
	testData, /*TODO: remove this dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data*/
	layerSelectionResolved
  ) { neuron ->
	layerSelectionResolved.value?.neurons?.firstOrNull { it.index == neuron?.index }
  }


  val imageSelection = VarProp<DeephyImage<*>?>(null)


  private val topNeuronsFromMyImage = run {
	imageSelection.binding(
	  testData, layerSelection/*, normalizeTopNeuronActivations*/, normalizer
	) { im ->
	  layerSelection.value?.let { lay ->
		im?.let { theIm ->
		  testData.value!!.dtype.topNeurons(
			images = contentsOf(theIm),
			layer = lay,
			/*normalized = normalizeTopNeuronActivations.value,*/
			test = testData.value!!.preppedTest.await(),
			denomTest = normalizer.value/*.takeIf { it != this }*/?.testData?.value?.preppedTest?.await()
		  )
		}
	  }
	}
  }


  val boundTopNeurons: MyBinding<TopNeurons<*>?> = boundToDSet.deepBinding(
	/*normalizeTopNeuronActivations,*/
	normalizer
  ) {


	it?.topNeurons?.binding(
	  /*normalizeTopNeuronActivations,*/ normalizer
	) {
	  it?.let {

		testData.value!!.dtype.topNeurons(
		  images = contentsOf(),
		  layer = it.layer,
		  test = testData.value!!.preppedTest.await(),
		  denomTest = normalizer.value?.testData?.value?.preppedTest?.await(),
		  /*normalized = normalizeTopNeuronActivations.value,*/
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


  private fun redoHistory() = when (val action = history[historyIndex.value]) {
	is SelectImage    -> navigateTo(action.image, addHistory = false)
	is SelectCategory -> navigateTo(action.cat, addHistory = false)
	is SelectNeuron   -> navigateTo(action.neuron, addHistory = false)
  }

  private val canUseHistory = this@DatasetViewer.history.binding(historyIndex, boundToDSet) {
	isUnboundToDSet.value && it.isNotEmpty()
  }

  init {
	contentDisplay = ContentDisplay.LEFT
	isExpanded = true
	/*titleProperty.bind(file.binding { it?.nameWithoutExtension })*/
	graphic = h {
	  alignment = Pos.CENTER
	  isFillHeight = true

	  /*spacing = 15.0*/

	  fun sectionSpacer() = h {
		minWidth = 30.0
		alignment = Pos.CENTER
		/*line {
		  strokeProperty.bindWeakly(DarkModeController.darkModeProp.binding { if (it) Color.DARKGRAY else Color.LIGHTGRAY })
		  endY = 25.0
		}*/
	  }

	  sectionSpacer()

	  val removeTestButton = deephyButton("-") {
		veryLazyDeephysTooltip("remove this test viewer")
		setOnAction {
		  this@DatasetViewer.outerBox.removeTest(this@DatasetViewer)
		}
	  }


	  /*"Choose Test"*/
	  val chooseTestButton = deephyButton("") {

		val ICON_LENGTH = 25

		graphic = imageview(
		  svgToImage2(
			resourceStream("open-file.svg")!!,
			width = ICON_LENGTH*2,
			height = ICON_LENGTH*2
		  ).toFXImage()
		) {
		  isPreserveRatio = true
		  fitWidth = ICON_LENGTH.toDouble()
		}
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


	  removeTestButton.prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)




	  sectionSpacer()


	  deephyButton("<-") {
		prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)
		enableWhen {
		  this@DatasetViewer.canUseHistory and this@DatasetViewer.historyIndex.gt(0)
		}
		setOnAction {
		  this@DatasetViewer.historyIndex.value -= 1
		  this@DatasetViewer.redoHistory()
		}

	  }
	  deephyButton("->") {
		prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)
		enableWhen {
		  this@DatasetViewer.canUseHistory and this@DatasetViewer.historyIndex.lt(this@DatasetViewer.history.lastIndexProperty)
		}
		setOnAction {
		  this@DatasetViewer.historyIndex.value += 1
		  this@DatasetViewer.redoHistory()
		}
	  }

	  sectionSpacer()

	  this@DatasetViewer.bindButton = this@DatasetViewer.outerBox.createBindToggleButton(this, this@DatasetViewer)
		.apply {
		  prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)
		}
	  this@DatasetViewer.oodButton = this@DatasetViewer.outerBox.createInDToggleButton(this, this@DatasetViewer).apply {
		prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)
	  }

	  sectionSpacer()

	  deephyText(this@DatasetViewer.file.binding { it?.nameWithoutExtension ?: "please select a test" }) {
		titleFont()
	  }
	  hSpacer(10.0)
	  deephysInfoSymbol(

		this@DatasetViewer.testData.binding {
		  if (it == null) {
			"After loading a test, see more info about it here."
		  } else {
			string {
			  lineDelimited {
				+"dtype:       ${it.dtype.label}"
				+"Image Count: ${it.numImages.await()}"
			  }
			}
		  }
		}

	  ) {


		fontProperty v DEEPHYS_FONT_MONO
		/*wrapTextProp v true*/
	  }

	  sectionSpacer()

	  progressbar {

		progressProperty.bind(this@DatasetViewer.testData.deepBinding {
		  it?.progress ?: 0.0.toVarProp()
		})
		visibleAndManagedWhen {
		  progressProperty.lt(1.0)
		}
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

