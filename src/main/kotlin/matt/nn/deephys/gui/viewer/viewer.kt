package matt.nn.deephys.gui.viewer

import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
import matt.caching.compcache.ComputeCacheContext
import matt.collect.itr.filterNotNull
import matt.collect.set.contents.contentsOf
import matt.collect.weak.lazyWeakMap
import matt.file.construct.mFile
import matt.file.ext.FileExtension
import matt.file.types.checkType
import matt.fx.control.inter.contentDisplay
import matt.fx.control.inter.graphic
import matt.fx.control.wrapper.control.ControlWrapper
import matt.fx.control.wrapper.progressbar.progressbar
import matt.fx.control.wrapper.titled.TitledPaneWrapper
import matt.fx.graphics.dialog.openFile
import matt.fx.graphics.fxthread.ts.nonBlockingFXWatcher
import matt.fx.graphics.wrapper.node.enableWhen
import matt.fx.graphics.wrapper.node.visibleAndManagedWhen
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperR
import matt.fx.graphics.wrapper.pane.hSpacer
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.lang.assertions.require.requireNot
import matt.lang.disabledCode
import matt.lang.model.file.MacFileSystem
import matt.lang.model.file.fName
import matt.lang.model.file.types.Cbor
import matt.lang.model.file.types.TypedFile
import matt.lang.weak.MyWeakRef
import matt.log.profile.stopwatch.stopwatch
import matt.log.profile.stopwatch.tic
import matt.log.warn.warn
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.gui.dataset.DatasetNode
import matt.nn.deephys.gui.dataset.DatasetNodeView
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.DEEPHYS_FONT_MONO
import matt.nn.deephys.gui.global.deephyIconButton
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.symbol.DEEPHYS_SYMBOL_SPACING
import matt.nn.deephys.gui.global.tooltip.symbol.DeephysWarningSymbol
import matt.nn.deephys.gui.global.tooltip.symbol.deephysInfoSymbol
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.action.SelectCategory
import matt.nn.deephys.gui.viewer.action.SelectImage
import matt.nn.deephys.gui.viewer.action.SelectNeuron
import matt.nn.deephys.gui.viewer.action.SelectView
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
import matt.obs.col.olist.lastIndexProperty
import matt.obs.col.olist.sizeProperty
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal
import matt.obs.prop.VarProp
import matt.obs.prop.toVarProp
import matt.obs.prop.withChangeListener
import matt.obs.prop.withNonNullUpdatesFrom
import matt.prim.str.mybuild.api.string
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

class DatasetViewer(
    initialFile: TypedFile<Cbor,*>? = null,
    val outerBox: DSetViewsVBox,
    settings: DeephysSettingsController,
    val cacheContext: ComputeCacheContext
) : TitledPaneWrapper() {

    private val initStopwatch = tic("viewer init", enabled = false)

    val showAsList1 = BindableProperty(false)
    val showAsList2 = BindableProperty(false)

    init {
        initStopwatch.toc(1)
    }

    val model by lazy { outerBox.model }

    val siblings by lazy { outerBox.children.filtered { it != this } }

    private val currentFile get() = file.value?.fName
    override fun reflectingToStringProps(): Set<KProperty<*>> {
        return setOf(::currentFile)
    }
//    override fun toString() = toStringBuilder("current file" to file.value?.fName)

    val file: VarProp<TypedFile<Cbor,*>?> = VarProp(initialFile).withChangeListener {
        outerBox.save()
    }

    val smallImageScale = BindableProperty(settings.appearance.smallImageScale.value).apply {
        bind(settings.appearance.smallImageScale)
    }
    val bigImageScale = BindableProperty(settings.appearance.bigImageScale.value).apply {
        bind(settings.appearance.bigImageScale)
    }

    //  val normalizeTopNeuronActivations = BindableProperty(DeephySettings.normalizeTopNeuronActivations.value).apply {
    //	bind(DeephySettings.normalizeTopNeuronActivations)
    //  }
    val numImagesPerNeuronInByImage = BindableProperty(settings.appearance.numImagesPerNeuronInByImage.value).apply {
        bind(settings.appearance.numImagesPerNeuronInByImage)
    }
    val averageRawActSigFigs = BindableProperty(settings.appearance.averageRawActSigFigs.value).apply {
        bind(settings.appearance.averageRawActSigFigs)
    }
    val predictionSigFigs = BindableProperty(settings.appearance.predictionSigFigs.value).apply {
        bind(settings.appearance.predictionSigFigs)
    }
    val showCacheBars = BindableProperty(settings.debug.showCacheBars.value).apply {
        bind(settings.debug.showCacheBars)
    }
    val showTutorials = BindableProperty(settings.showTutorials.value).apply {
        bind(settings.showTutorials)
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

            val loader = TestLoader(f, model, settings)
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
        testData
    ) { layer ->
        println("remove testData dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data")
        model.resolvedLayers.firstOrNull { it.layerID == layer?.layerID }
    }

    private val boundNeuron = boundToDSet.deepBindingIgnoringFutureNullOuterChanges {
        it?.neuronSelection
    }

    private fun ResolvedLayer.neuronThatMatches(n: ResolvedNeuron?) = neurons.firstOrNull {
        it.index == n?.index
    }


    val neuronSelection: VarProp<InterTestNeuron?> = VarProp(
        boundNeuron.value
    ).withNonNullUpdatesFrom(boundNeuron)

    val neuronSelectionResolved = neuronSelection.binding(
        testData,
        layerSelectionResolved
    ) { neuron ->
        println("remove layerSelectionResolved dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data")
        layerSelectionResolved.value?.neurons?.firstOrNull { it.index == neuron?.index }
    }


    val imageSelection = VarProp<DeephyImage<*>?>(null)


    private val topNeuronsFromMyImage = run {
        imageSelection.binding(
            testData, layerSelection, normalizer
        ) { im ->
            layerSelection.value?.let { lay ->
                im?.let { theIm ->
                    testData.value!!.dtype.topNeurons(
                        images = contentsOf(theIm),
                        layer = lay,
                        test = testData.value!!.preppedTest.awaitRequireSuccessful(),
                        denomTest = normalizer.value?.testData?.value?.preppedTest?.awaitRequireSuccessful()
                    )
                }
            }
        }
    }


    val boundTopNeurons: MyBinding<TopNeurons<*>?> = boundToDSet.deepBinding(
        normalizer
    ) {
        it?.topNeurons?.binding(
            normalizer
        ) {
            it?.let {
                testData.value?.dtype?.topNeurons(
                    images = contentsOf(),
                    layer = it.layer,
                    test = testData.value!!.preppedTest.awaitRequireSuccessful(),
                    denomTest = normalizer.value?.testData?.value?.preppedTest?.awaitRequireSuccessful(),
                    forcedNeuronIndices = with(cacheContext) { with(testData.value!!.testRAMCache) { it() }.map { it.neuron.index } }
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
            ByImage    -> with(testData.value!!.testRAMCache) { topNeurons.value?.findOrCompute() ?: listOf() }
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

    fun navigateTo(
        neuron: InterTestNeuron,
        addHistory: Boolean = true
    ) {
        requireNot(isBoundToDSet.value)
        neuronSelection.value = null
        layerSelection.value = neuron.layer
        neuronSelection.value = neuron
        if (addHistory) appendHistory(SelectNeuron(neuron))
        view.value = ByNeuron
    }


    fun navigateTo(
        im: DeephyImage<*>,
        addHistory: Boolean = true
    ) {
        if (isBoundToDSet.value) outerBox.selectViewerToBind(null)
        imageSelection.value = im
        if (addHistory) appendHistory(SelectImage(im))
        view.value = ByImage
    }

    fun navigateTo(
        category: CategorySelection,
        addHistory: Boolean = true
    ) {
        if (isBoundToDSet.value) outerBox.selectViewerToBind(null)
        neuronSelection.value = null
        categorySelection.value = category
        if (addHistory) appendHistory(SelectCategory(category))
        view.value = ByCategory
    }

    fun navigateTo(
        theView: DatasetNodeView,
        addHistory: Boolean = true
    ) {
        when (theView) {
            ByCategory -> {
                if (categorySelection.value == null) {
                    val cats = testData.value?.test?.categories
                    if (cats?.isNotEmpty() == true) {
                        categorySelection.value = cats.first()
                    }
                }
            }

            ByImage    -> {
                if (imageSelection.value == null) {
                    val ims = testData.value?.test?.images
                    if (ims?.isNotEmpty() == true) {
                        imageSelection.value = ims.first()
                    }
                }
            }

            ByNeuron   -> Unit /*we start here, so don't worry about this right now*/
        }

        if (isBoundToDSet.value) outerBox.selectViewerToBind(null)
        if (addHistory) appendHistory(SelectView(theView))
        view.value = theView
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
        is SelectView     -> navigateTo(action.view, addHistory = false)
    }

    private val canUseHistory = this@DatasetViewer.history.binding(historyIndex, boundToDSet) {
        isUnboundToDSet.value && it.isNotEmpty()
    }


    init {
        val weakViewer = WeakReference(this)
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

            val removeTestButton = deephyIconButton("icon/minus") {
                veryLazyDeephysTooltip("remove this test viewer", settings)
                setOnAction {
                    this@DatasetViewer.outerBox.removeTest(this@DatasetViewer)
                }
            }


            /*"Choose Test"*/
            val chooseTestButton = deephyIconButton("open-file") {


                veryLazyDeephysTooltip("choose test file", settings)
                setOnAction {

                    val f = openFile(stage = weakViewer.get()!!.stage) {
                        title = "choose test data"
                        extensionFilter("tests", FileExtension.TEST)
                    }

                    if (f != null) {
                        stopwatch("set fileProp") {
                            this@DatasetViewer.file.value = (mFile(f.path, MacFileSystem)).checkType()
                        }
                    }
                }
            }


            removeTestButton.prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)




            sectionSpacer()


            deephyIconButton("icon/arrow") {
                graphic!!.apply {
                    scaleX = -1.0
                }


                prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)

                enableWhen {
                    this@DatasetViewer.canUseHistory and this@DatasetViewer.historyIndex.gt(0)
                }
                setOnAction {
                    this@DatasetViewer.historyIndex.value -= 1
                    this@DatasetViewer.redoHistory()
                }

            }
            deephyIconButton("icon/arrow") {
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
            this@DatasetViewer.oodButton =
                this@DatasetViewer.outerBox.createInDToggleButton(this, this@DatasetViewer).apply {
                    prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)
                }

            sectionSpacer()


            /*.binding { it?.nameWithoutExtension ?: "please select a test" }*/

            deephysText(
                this@DatasetViewer.testData.binding {
                    it?.testName?.awaitSuccessfulOrMessage()?.toString() ?: "please select a test"
                }
            ) {
                titleFont()
            }
            hSpacer(10.0)
            h {
                spacing = DEEPHYS_SYMBOL_SPACING
                deephysInfoSymbol(

                    this@DatasetViewer.testData.binding {
                        if (it == null) {
                            "After loading a test, see more info about it here."
                        } else {
                            string {
                                lineDelimited {
                                    +"dtype:       ${it.dtypeOrNull()?.label}"
                                    +"Image Count: ${it.numImages.awaitSuccessfulOrMessage()}"
                                }
                            }
                        }
                    }

                ) {


                    fontProperty v DEEPHYS_FONT_MONO
                    /*wrapTextProp v true*/
                }

                val loadWarnings = this@DatasetViewer.testData.binding {
                    it?.loadWarnings
                }

                swapperR(loadWarnings) {
                    h {
                        spacing = DEEPHYS_SYMBOL_SPACING
                        children.bindWeakly(it.nonBlockingFXWatcher()) {
                            DeephysWarningSymbol(it).apply {

                            }
                        }
                    }
                }
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
                this@DatasetViewer.testData.binding { it },
                nullMessage = "select a test to view it",
                fadeOutDur = DEEPHYS_FADE_DUR,
                fadeInDur = DEEPHYS_FADE_DUR
            ) {
                DatasetNode(this, this@DatasetViewer, settings)
            }
            disabledCode {
                +BindTutorial(this@DatasetViewer)
            }
        }
    }


}

