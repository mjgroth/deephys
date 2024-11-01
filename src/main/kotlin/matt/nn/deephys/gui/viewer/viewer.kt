package matt.nn.deephys.gui.viewer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import matt.caching.compcache.ComputeCacheContext
import matt.caching.compcache.findOrCompute
import matt.caching.compcache.invoke
import matt.collect.itr.filterNotNull
import matt.collect.weak.lazy.lazyWeakMap
import matt.compose.graphics.layout.AlignedRow
import matt.file.model.file.types.Cbor
import matt.file.model.file.types.TypedFile
import matt.lang.assertions.require.requireNot
import matt.lang.common.unsafeErr
import matt.lang.common.unsafeReturningErr
import matt.lang.model.file.fName
import matt.lang.weak.weak
import matt.log.profile.stopwatch.tic
import matt.log.warn.common.warn
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.gui.dataset.DatasetNodeView
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.unsafemigration.ControlWrapper
import matt.nn.deephys.gui.viewer.action.SelectCategory
import matt.nn.deephys.gui.viewer.action.SelectImage
import matt.nn.deephys.gui.viewer.action.SelectNeuron
import matt.nn.deephys.gui.viewer.action.SelectView
import matt.nn.deephys.gui.viewer.action.TestViewerAction
import matt.nn.deephys.load.test.PostDtypeTestLoader
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.load.test.dtype.topNeurons
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.ResolvedNeuron
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.layer.Layer
import matt.nn.deephys.model.importformat.neuron.Neuron
import matt.obs.bind.MyBinding
import matt.obs.bind.binding
import matt.obs.bind.coalesceNull
import matt.obs.bind.deepBindingIgnoringFutureNullOuterChanges
import matt.obs.bind.weakBinding
import matt.obs.bindings.bool.not
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.prop.ObsVal
import matt.obs.prop.withChangeListener
import matt.obs.prop.writable.BindableProperty
import matt.obs.prop.writable.VarProp
import matt.obs.prop.writable.withNonNullUpdatesFrom


class DatasetViewerState(
    initialFile: TypedFile<Cbor, *>? = null,
    val outerBox: DSetViewsState,
    settings: DeephysSettingsController,
    val cacheContext: ComputeCacheContext
) {
    val showAsList1 = BindableProperty(false)
    val showAsList2 = BindableProperty(false)
    val model by lazy {  unsafeErr("outerBox.model") }
    val siblings by lazy {
        unsafeErr(
            """
            outerBox.datasets.filtered { it != this }        
            """.trimIndent()
        )
    }
    private val currentFile get() = file.value?.fName


    val file: VarProp<TypedFile<Cbor, *>?> =
        VarProp(initialFile).withChangeListener {
            outerBox.save()
        }

    val smallImageScale =
        derivedStateOf {
            settings.appearance.smallImageScale.value
        }
    val bigImageScale =
        derivedStateOf {
            settings.appearance.bigImageScale.value
        }

    val numImagesPerNeuronInByImage =
        derivedStateOf {
            settings.appearance.numImagesPerNeuronInByImage.value
        }
    val averageRawActSigFigs =
        derivedStateOf {
            settings.appearance.averageRawActSigFigs.value
        }
    val predictionSigFigs =
        derivedStateOf {
            (settings.appearance.predictionSigFigs).value
        }
    val showCacheBars =
        derivedStateOf {
            (settings.debug.showCacheBars).value
        }
    val showTutorials =
        derivedStateOf {
            (settings.showTutorials).value
        }

    val normalizer =
        derivedStateOf {
            (outerBox.normalizer).value
        }

    val numViewers =
        derivedStateOf {
            (outerBox.datasets.size)
        }

    val testData =
        file.binding { f ->
            val t = tic(prefix = "dataBinding2", enabled = false)
            t.toc("start")
            f?.run {

                val loader = TestLoader(f, unsafeReturningErr("model"), settings)
                t.toc("got loader")
                loader.start()
                t.toc("started loader")
                loader
            }
        }


    val outerBoundDSet =
        BindableProperty(outerBox.bound.value).apply {
            bind(unsafeReturningErr { outerBox.bound })
        }

    val boundToDSet by lazy {
        outerBoundDSet.binding {
            if (it != this@DatasetViewerState) it else null
        }
    }
    val isBoundToDSet by lazy { boundToDSet.isNotNull }
    val isUnboundToDSet by lazy { isBoundToDSet.not() }


    private val boundView by lazy { boundToDSet.deepBindingIgnoringFutureNullOuterChanges { it?.view } }
    val view: VarProp<DatasetNodeView> =
        VarProp(
            boundView.value ?: ByNeuron
        ).withNonNullUpdatesFrom(boundView)

    private val boundLayer by lazy { boundToDSet.deepBindingIgnoringFutureNullOuterChanges { it?.layerSelection } }

    val layerSelection: VarProp<InterTestLayer?> =
        VarProp(
            boundLayer.value
        ).withNonNullUpdatesFrom(boundLayer)

    val layerSelectionResolved: ObsVal<Layer?> =
        layerSelection.binding(
            testData
        ) { layer ->
            println(
                "remove testData dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data"
            )
            unsafeReturningErr(
                """
                model.resolvedLayers.firstOrNull { it.layerID == layer?.layerID }    
                """.trimIndent()
            )
        }

    private val boundNeuron: ObsVal<Neuron?> =
        boundToDSet.deepBindingIgnoringFutureNullOuterChanges {
            unsafeReturningErr {
                it?.neuronSelection
            }
        }

    private fun ResolvedLayer.neuronThatMatches(n: ResolvedNeuron?) =
        neurons.firstOrNull {
            it.index == n?.index
        }


    val neuronSelection: MutableState<InterTestNeuron?> =
        unsafeReturningErr(
            """
            mutableStateOf(
                boundNeuron.value
            ).withNonNullUpdatesFrom(boundNeuron)    
            """.trimIndent()
        )


    val neuronSelectionResolved =
        derivedStateOf {
            testData.value /*reset this state when test changes?*/
            val neuron = neuronSelection.value

            println(
                "remove layerSelectionResolved dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data"
            )

            layerSelectionResolved.value?.neurons?.firstOrNull {
                unsafeReturningErr(
                    """
                    it.index == neuron?.index        
                    """.trimIndent()
                )
            }
        }




    val imageSelection = VarProp<DeephyImage<*>?>(null)


    private val topNeuronsFromMyImage: ObsVal<TopNeurons<*>?> =
        unsafeReturningErr(
            """
            run {
                imageSelection.binding(
                    testData,
                    layerSelection,
                    normalizer
                ) { im ->
                    layerSelection.value?.let { lay ->
                        im?.let { theIm ->
                            val prepped1 = testData.value!!.postDtypeTestLoader.awaitRequireSuccessful().preppedTest
                            val prepped2 = normalizer.value?.testData?.value?.postDtypeTestLoader?.awaitRequireSuccessful()?.preppedTest
                            val prepped1Got = prepped1.awaitRequireSuccessful()
                            check(prepped1Got.test == theIm.testLoader.test)
                            val testWithOneImagesHack = theIm.withTest()
                            topNeurons(
                                testAndImages =  testWithOneImagesHack,
                                layer = lay,
                                denomTest = prepped2?.awaitRequireSuccessful()
                            )
                        }
                    }
                }
            }          
            """.trimIndent()
        )



    val boundTopNeurons: MyBinding<TopNeurons<*>?> =
        unsafeReturningErr(
            """
            boundToDSet.deepBinding(
                normalizer
            ) {
                it?.topNeurons?.binding(
                    normalizer
                ) {
                    it?.let {
                        val prepped1 = testData.value!!.postDtypeTestLoader.awaitRequireSuccessful().preppedTest
                        val prepped2 = normalizer.value?.testData?.value?.postDtypeTestLoader?.awaitRequireSuccessful()?.preppedTest
                        val prepped1Got = prepped1.awaitRequireSuccessful()
                        val testWithNoImages = prepped1Got.withNoImages()
                        topNeurons(
                            testAndImages = testWithNoImages,
                            layer = it.layer,
                            denomTest = prepped2?.awaitRequireSuccessful(),
                            forcedNeuronIndices = with(cacheContext) { with(testData.value!!.testRAMCache) { it() }.map { it.neuron.index } }
                        )
                    }
                } ?: BindableProperty(null)
            }       
            """.trimIndent()
        )



    val topNeurons: MyBinding<TopNeurons<*>?> = boundTopNeurons coalesceNull unsafeReturningErr { topNeuronsFromMyImage }


    val highlightedNeurons =
        derivedStateOf {
            when (view.value) {
                ByNeuron   -> listOf(neuronSelection.value).filterNotNull()

                ByImage    -> with(testData.value!!.testRAMCache) { topNeurons.value?.findOrCompute() ?: listOf() }

                ByCategory ->
                    listOf<InterTestNeuron>().apply {
                        warn("did not make highlighted neurons from category view work yet")
                    }
            }
        }
    val weakRef = weak(this)

    private val boundCategory: ObsVal<CategorySelection?> =
        boundToDSet
            .deepBindingIgnoringFutureNullOuterChanges(testData) {
                it?.catSelectionForViewer?.get(weakRef.deref()!!) ?: BindableProperty(null)
            }


    val categorySelection = VarProp<CategorySelection?>(null).withNonNullUpdatesFrom(boundCategory)

    private val catSelectionForViewer =
        lazyWeakMap<DatasetViewerState, ObsVal<CategorySelection?>> { viewer ->
            categorySelection.weakBinding(viewer) { v, cat ->
                v.testData.value?.let { tst ->
                    cat?.forTest(tst)
                }
            }
        }


    val currentByImageHScroll = mutableStateOf<ScrollState?>(null)

    val history = basicMutableObservableListOf<TestViewerAction>()
    val historyIndex = VarProp(-1)


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

    var bindButton: ControlWrapper? = null
    var oodButton: ControlWrapper? = null


    private fun redoHistory() =
        when (val action = history[historyIndex.value]) {
            is SelectImage    -> navigateTo(action.image, addHistory = false)
            is SelectCategory -> navigateTo(action.cat, addHistory = false)
            is SelectNeuron   -> navigateTo(action.neuron, addHistory = false)
            is SelectView     -> navigateTo(action.view, addHistory = false)
        }

    private val canUseHistory =
        this@DatasetViewerState.history.binding(historyIndex, boundToDSet) {
            isUnboundToDSet.value && it.isNotEmpty()
        }
}


@Composable
fun DatasetViewer(
    state: DatasetViewerState
) {
    unsafeErr(
        """
        CollapsePane(
            expanded = rememberMutableStateOf(true)
        ) {
            val weakViewer = WeakReference(this)
            contentDisplay = ContentDisplay.LEFT
            /*titleProperty.bind(file.binding { it?.nameWithoutExtension })*/
            graphic =
                AlignedRow(horizontalArrangement = Arrangement.Center) {
                    SectionSpacer()

                    val removeTestButton =
                        DeephyIconButton("icon/minus") {
                            veryLazyDeephysTooltip("remove this test viewer", settings)
                            setOnAction {
                                this@DatasetViewer.outerBox.removeTest(this@DatasetViewer)
                            }
                        }

                    DeephysTooltipArea(settings, "choose test file") {
                        /*"Choose Test"*/
                        val chooseTestButton =
                            DeephyIconButton("open-file") {


                                unsafeErr(
                                    ""${'"'}
                                        val f =
                                        openFile(stage = weakViewer.get()!!.stage) {
                                            title = "choose test data"
                                            extensionFilter("tests", FileExtension.TEST)
                                        }

                                    if (f != null) {
                                        stopwatch("set fileProp") {
                                            this@DatasetViewer.file.value = (mFile(f.path, MacFileSystem)).checkType(Cbor)
                                        }
                                    }
                                    ""${'"'}.trimIndent()
                                )
                            }
                    }



                    removeTestButton.prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)




                    SectionSpacer()


                    DeephyIconButton("icon/arrow") {
                        unsafeErr(
                            ""${'"'}
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
                            ""${'"'}.trimIndent()
                        )
                    }
                    DeephyIconButton("icon/arrow") {
                        unsafeErr(
                            ""${'"'}
                               prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)
                            enableWhen {
                                (
                                    this@DatasetViewer.canUseHistory and
                                        this@DatasetViewer.historyIndex.lt(
                                            this@DatasetViewer.history.lastIndexProperty
                                        )
                                )
                            }
                            setOnAction {
                                this@DatasetViewer.historyIndex.value += 1
                                this@DatasetViewer.redoHistory()
                            }
                                    
                            ""${'"'}.trimIndent()
                        )
                    }

                    SectionSpacer()

                    this@DatasetViewer.bindButton =
                        this@DatasetViewer.outerBox.createBindToggleButton(this, this@DatasetViewer)
                            .apply {
                                prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)
                            }
                    this@DatasetViewer.oodButton =
                        this@DatasetViewer.outerBox.createInDToggleButton(this, this@DatasetViewer).apply {
                            prefHeightProperty.bindWeakly(chooseTestButton.heightProperty)
                        }

                    SectionSpacer()


                    /*.binding { it?.nameWithoutExtension ?: "please select a test" }*/

                    DeephysText(
                        this@DatasetViewer.testData.binding {
                            it?.testName?.awaitSuccessfulOrMessage()?.toString() ?: "please select a test"
                        },
                        font = titleFont()
                    )
                    Spacer(Modifier.width(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(DEEPHYS_SYMBOL_SPACING.dp)) {
                        DeephysInfoSymbol(

                            this@DatasetViewer.testData.binding {
                                if (it == null) {
                                    "After loading a test, see more info about it here."
                                } else {
                                    string {
                                        lineDelimited {
                                            +"dtype:       ${'$'}{it.dtypeOrNull()?.label}"
                                            +"Image Count: $${'$'}{it.numImages.awaitSuccessfulOrMessage()}"
                                        }
                                    }
                                }
                            }

                        ) {


                            fontProperty v MONO_FONT
                            /*wrapTextProp v true*/
                        }

                        val loadWarnings = testData.loadWarnings

                        Row(horizontalArrangement = Arrangement.spacedBy(DEEPHYS_SYMBOL_SPACING.dp)) {

                            children.bindWeakly(it.nonBlockingFXWatcher()) {
                                DeephysWarningSymbol(loadWarnings).apply {
                                }
                            }
                        }
                    }

                    SectionSpacer()


                    progressbar {

                        progressProperty.bind(
                            this@DatasetViewer.testData.deepBinding {
                                it?.progress?.progress ?: 0.0.toVarProp()
                            }
                        )
                        visibleAndManagedWhen {
                            progressProperty.lt(1.0)
                        }
                    }
                    progressbar {
                        visibleAndManagedWhen { this@DatasetViewer.showCacheBars }
                        style = "-fx-accent: green"
                        progressProperty.bind(
                            this@DatasetViewer.testData.deepBinding {
                                it?.progress?.cacheProgressPixels ?: 0.0.toVarProp()
                            }
                        )
                    }
                    progressbar {
                        visibleAndManagedWhen { this@DatasetViewer.showCacheBars }
                        style = "-fx-accent: yellow"
                        progressProperty.bind(
                            this@DatasetViewer.testData.deepBinding {
                                it?.progress?.cacheProgressActs ?: 0.0.toVarProp()
                            }
                        )
                    }
                }
            content =
                Column {
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
        """.trimIndent()
    )
}




class DtypedDatasetViewer<A: Number>(post: PostDtypeTestLoader<A>)


@Composable
private fun SectionSpacer() =
    AlignedRow(
        Modifier
            .requiredWidthIn(min = 30.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        /*line {
          strokeProperty.bindWeakly(DarkModeController.darkModeProp.binding { if (it) Color.DARKGRAY else Color.LIGHTGRAY })
          endY = 25.0
        }*/
    }
