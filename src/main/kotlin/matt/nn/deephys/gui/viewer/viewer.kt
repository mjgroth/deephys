@file:Suppress("UNUSED_VARIABLE")

package matt.nn.deephys.gui.viewer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import matt.caching.compcache.findOrCompute
import matt.caching.compcache.globalman.ComputeCacheContext
import matt.caching.compcache.invoke
import matt.collect.itr.filterNotNull
import matt.compose.controls.dialog.filechoose.RealComposeFileChooser
import matt.compose.graphics.layout.AlignedRow
import matt.compose.graphics.padding.WidthSpacer
import matt.compose.snap.state.mutate.setToNull
import matt.compose.snap.withSafeMutableSnapshot
import matt.compose.state.readonly.readOnly
import matt.compose.state.shortcuts.rememberMutableStateOfFalse
import matt.file.JioFile
import matt.file.construct.toJioFile
import matt.file.ext.FileExtension
import matt.file.ext.singleExtensionOrNullIfNoDots
import matt.lang.anno.optin.UnsafeMattCode
import matt.lang.assertions.require.requireNot
import matt.lang.codecomment.disabledCode
import matt.lang.err.unsafeReturningErr
import matt.lang.nop.DoNothing
import matt.log.profile.stopwatch.stopwatch
import matt.log.profile.stopwatch.tic
import matt.model.k.log.Logger
import matt.model.k.log.warnPrefixed
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.gui.dataset.DatasetNode
import matt.nn.deephys.gui.dataset.DatasetNodeView
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
import matt.nn.deephys.gui.fix.withNoImages
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.DeephyIconButton
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.global.tooltip.symbol.DEEPHYS_SYMBOL_SPACING
import matt.nn.deephys.gui.global.tooltip.symbol.DeephysInfoSymbol
import matt.nn.deephys.gui.global.tooltip.symbol.DeephysWarningSymbol
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.unsafemigration.ControlWrapper
import matt.nn.deephys.gui.viewer.action.SelectCategory
import matt.nn.deephys.gui.viewer.action.SelectImage
import matt.nn.deephys.gui.viewer.action.SelectNeuron
import matt.nn.deephys.gui.viewer.action.SelectView
import matt.nn.deephys.gui.viewer.action.TestViewerAction
import matt.nn.deephys.gui.viewer.tutorial.bind.BindTutorial
import matt.nn.deephys.load.AsyncLoadSwapper
import matt.nn.deephys.load.test.PostDtypeTestLoader
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.load.test.dtype.topNeurons
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.ResolvedNeuron
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.obs.bindings.comp.gt
import matt.obs.prop.ObsVal
import matt.obs.prop.writable.VarProp
import matt.prim.str.mybuild.api.string
import matt.prim.weak.weak

class DatasetViewerState(
    initialCborFile: JioFile? = null,
    val outerBox: DSetViewsState,
    settings: DeephysSettingsController,
    val cacheContext: ComputeCacheContext,
    scope: CoroutineScope,
    logger: Logger
) {
    val showAsList1 = mutableStateOf(false)
    val showAsList2 = mutableStateOf(false)
    val model =
        derivedStateOf {
            outerBox.modelVisualizer.model.value
        }
    @Suppress("unused")
    val siblings =
        derivedStateOf {
            outerBox.datasets.filter { it != this }
        }
    @Suppress("unused")
    private val currentFile get() = file.value?.fName

    val _file = mutableStateOf(initialCborFile)
    val file = _file.readOnly()
    fun setCborFile(file: JioFile) {
        withSafeMutableSnapshot {
            _file.value = file
            outerBox.save()
        }
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
        derivedStateOf {
            val f = file.value
            val t = tic(prefix = "dataBinding2", enabled = false)
            t.toc("start")
            f?.run {

                val loader = TestLoader(f, unsafeReturningErr("model"), settings, logger)
                t.toc("got loader")
                loader.start()
                t.toc("started loader")
                loader
            }
        }

    val outerBoundDSet =
        derivedStateOf {
            outerBox.bound.value
        }

    val boundToDSet =
        derivedStateOf {
            val it = outerBoundDSet.value
            if (it != this@DatasetViewerState) it else null
        }
    private val isBoundToDSet = derivedStateOf { boundToDSet.value != null }
    val isUnboundToDSet = derivedStateOf { !isBoundToDSet.value }

    private val boundView: State<DatasetNodeView?> =
        derivedStateOf {
            boundToDSet.value?.view?.value
        }
    val manuallySelectedView =
        mutableStateOf(
            boundView.value ?: ByNeuron
        )

    val view =
        derivedStateOf {
            boundView.value ?: manuallySelectedView.value
        }

    private val boundLayer: State<InterTestLayer?> =
        derivedStateOf {
            boundToDSet.value?.layerSelection?.value
        }

    val manualLayerSelected: MutableState<InterTestLayer?> = mutableStateOf(boundLayer.value)

    val layerSelection: State<InterTestLayer?> =
        derivedStateOf {
            boundLayer.value ?: manualLayerSelected.value
        }

    @Suppress("unused")
    val layerSelectionResolved: State<ResolvedLayer?> =
        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        derivedStateOf {
            val layer = layerSelection.value
            model.value?.resolvedLayers?.firstOrNull { it.layerID == layer?.layerID }
        }

    @Suppress("unused")
    private val boundNeuron =
        derivedStateOf {
            boundToDSet.value?.neuronSelection?.value
        }

    @Suppress("unused")
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

    @Suppress("unused")
    val neuronSelectionResolved =
        derivedStateOf {
            testData.value /*reset this state when test changes?*/
            val neuron = neuronSelection.value

            println(
                "remove layerSelectionResolved dependency. more cleanly separate model from test. Selected layer should have nothing to do with the test data"
            )

            layerSelectionResolved.value?.run {
                neurons.firstOrNull {
                    unsafeReturningErr(
                        """
                                it.index == neuron?.index        
                        """.trimIndent()
                    )
                }
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

    private val boundTopNeurons: State<TopNeurons<*>?> =
        derivedStateOf {
            val tn = boundToDSet.value?.topNeurons?.value
            if (tn != null) {
                val prepped1 = testData.value!!.postDtypeTestLoader.awaitRequireSuccessful().preppedTest
                val prepped2 = normalizer.value?.testData?.value?.postDtypeTestLoader?.awaitRequireSuccessful()?.preppedTest
                val prepped1Got = prepped1.awaitRequireSuccessful()
                val testWithNoImages = prepped1Got.withNoImages()
                topNeurons(
                    testAndImages = testWithNoImages,
                    layer = tn.layer,
                    denomTest = prepped2?.awaitRequireSuccessful(),
                    forcedNeuronIndices =
                        /*with(cacheContext) {*/
                        with(testData.value!!.testRAMCache) {
                            tn()
                        }.map { it.neuron.index }
                    /*}*/
                )
            } else {
                null
            }
        }

    private val topNeurons: State<TopNeurons<*>?> =
        derivedStateOf {
            boundTopNeurons.value ?: topNeuronsFromMyImage.value
        }

    @Suppress("unused")
    val highlightedNeurons =
        derivedStateOf {
            when (view.value) {
                ByNeuron   -> arrayOf(neuronSelection.value).filterNotNull()

                ByImage    -> with(testData.value!!.testRAMCache) { topNeurons.value?.findOrCompute() ?: listOf() }

                ByCategory ->
                    listOf<InterTestNeuron>().apply {
                        with(logger) {
                            warnPrefixed("did not make highlighted neurons from category view work yet")
                        }
                    }
            }
        }
    val weakRef = weak(this)

    private val manuallySelectedCategory = mutableStateOf<CategorySelection?>(null)
    fun selectCategory(category: CategorySelection) {
        manuallySelectedCategory.value = category
    }

    val boundCategory =
        derivedStateOf {
            val b = boundToDSet.value
            if (b == null) {
                testData.value?.let { tst ->
                    manuallySelectedCategory.value?.forTest(tst)
                }
            } else {
                b.testData.value?.let { tst ->
                    b.manuallySelectedCategory.value?.forTest(tst)
                }
            }
        }

    val currentByImageHScroll = mutableStateOf<ScrollState?>(null)

    val history = mutableStateListOf<TestViewerAction>()
    val historyIndex = VarProp(-1)

    private fun appendHistory(historyAction: TestViewerAction) {
        for (@Suppress("unused") i in (historyIndex.value + 1)..<history.size) history.removeAt(historyIndex.value + 1)
        history.add(historyAction)
        historyIndex.value += 1
    }

    fun navigateTo(
        neuron: InterTestNeuron,
        addHistory: Boolean = true
    ) {
        requireNot(isBoundToDSet.value)
        neuronSelection.setToNull()
        manualLayerSelected.value = neuron.layer
        neuronSelection.value = neuron
        if (addHistory) appendHistory(SelectNeuron(neuron))
        manuallySelectedView.value = ByNeuron
    }

    fun navigateTo(
        im: DeephyImage<*>,
        addHistory: Boolean = true
    ) {
        if (isBoundToDSet.value) outerBox.selectViewerToBind(null)
        imageSelection.value = im
        if (addHistory) appendHistory(SelectImage(im))
        manuallySelectedView.value = ByImage
    }

    fun navigateTo(
        category: CategorySelection,
        addHistory: Boolean = true
    ) {
        if (isBoundToDSet.value) outerBox.selectViewerToBind(null)
        neuronSelection.setToNull()
        selectCategory(category)
        if (addHistory) appendHistory(SelectCategory(category))
        manuallySelectedView.value = ByCategory
    }

    fun navigateTo(
        theView: DatasetNodeView,
        addHistory: Boolean = true
    ) {
        when (theView) {
            ByCategory -> {
                if (boundCategory.value == null) {
                    val cats = testData.value?.run { test.categories }
                    if (cats?.isNotEmpty() == true) {
                        selectCategory(cats.first())
                    }
                }
            }

            ByImage    -> {
                if (imageSelection.value == null) {
                    val ims = testData.value?.run { test.images }
                    if (ims?.isNotEmpty() == true) {
                        imageSelection.value = ims.first()
                    }
                }
            }

            ByNeuron   -> DoNothing /*we start here, so don't worry about this right now*/
        }

        if (isBoundToDSet.value) outerBox.selectViewerToBind(null)
        if (addHistory) appendHistory(SelectView(theView))
        manuallySelectedView.value = theView
    }

    var bindButton: ControlWrapper? = null
    var oodButton: ControlWrapper? = null

    fun redoHistory() =
        when (val action = history[historyIndex.value]) {
            is SelectImage    -> navigateTo(action.image, addHistory = false)
            is SelectCategory -> navigateTo(action.cat, addHistory = false)
            is SelectNeuron   -> navigateTo(action.neuron, addHistory = false)
            is SelectView     -> navigateTo(action.view, addHistory = false)
        }

    val canUseHistory =
        derivedStateOf {
            isUnboundToDSet.value && history.isNotEmpty()
        }
}

@OptIn(UnsafeMattCode::class)
@Suppress("UnusedParameter")
@Composable
context(_: Logger)
fun DatasetViewer1(
    state: DatasetViewerState,
    settings: DeephysSettingsController
) {

    AlignedRow(horizontalArrangement = Arrangement.Center) {
        SectionSpacer()
        @Suppress("unused")
        val removeTestButton =
            DeephysTooltipArea(settings, "remove this test viewer") {
                DeephyIconButton("icon/minus") {
                    state.outerBox.removeTest(state)
                }
            }

        val showFileChooser = rememberMutableStateOfFalse()
        DeephysTooltipArea(settings, "choose test file") {

            /*"Choose Test"*/
            @Suppress("unused")
            val chooseTestButton =
                DeephyIconButton("open-file") {
                    showFileChooser.value = true
                }
        }

        if (showFileChooser.value) {
            RealComposeFileChooser(
                new = false,
                title = "choose test data",
                isValid = {
                    it.singleExtensionOrNullIfNoDots == FileExtension.TEST
                },
                onDismiss = {},
                onChoose = { f ->
                    stopwatch("set fileProp") {
                        state.setCborFile(f.toJioFile())
                    }
                }
            )
        }
        SectionSpacer()

        DeephyIconButton(
            "icon/arrow",
            Modifier.rotate(180f),
            enabled = state.canUseHistory.value and state.historyIndex.gt(0).value
        ) {
            withSafeMutableSnapshot {
                state.historyIndex.value -= 1
                state.redoHistory()
            }
        }

        DeephyIconButton(
            "icon/arrow",
            enabled = state.canUseHistory.value && (state.historyIndex.value < state.history.lastIndex)
        ) {
            withSafeMutableSnapshot {
                state.historyIndex.value += 1
                state.redoHistory()
            }
        }

        SectionSpacer()

        state.bindButton = state.outerBox.createBindToggleButton(this, state)
        state.oodButton = state.outerBox.createInDToggleButton(this, state)

        SectionSpacer()

        /*.binding { it?.nameWithoutExtension ?: "please select a test" }*/

        val testData = state.testData
        DeephysText(
            remember(testData) {
                derivedStateOf {
                    testData.value?.run {
                        testName.awaitSuccessfulOrMessage().toString()
                    } ?: "please select a test"
                }
            },
            style = titleFont()
        )
        WidthSpacer(10.dp)

        Row(horizontalArrangement = Arrangement.spacedBy(DEEPHYS_SYMBOL_SPACING.dp)) {
            DeephysInfoSymbol(

                state.testData.value.let { testData ->
                    remember(testData) {
                        if (testData == null) {
                            "After loading a test, see more info about it here."
                        } else {
                            string {
                                lineDelimited {
                                    +"dtype:       ${testData.dtypeOrNull()?.label}"
                                    +"Image Count: ${testData.numImages.awaitSuccessfulOrMessage()}"
                                }
                            }
                        }
                    }
                }

            )

            val loadWarnings = state.testData.value!!.loadWarnings

            Row(horizontalArrangement = Arrangement.spacedBy(DEEPHYS_SYMBOL_SPACING.dp)) {
                loadWarnings.forEach {
                    DeephysWarningSymbol(it)
                }
            }
        }

        SectionSpacer()

        val prog1 = state.testData.value?.progress?.progress?.value?.toFloat() ?: 0f
        if (prog1 < 1.0) {
            LinearProgressIndicator({
                prog1
            })
        }

        if (state.showCacheBars.value) {
            val prog2 = state.testData.value?.progress?.cacheProgressPixels?.value?.toFloat() ?: 0f
            LinearProgressIndicator({
                prog2
            }, color = Color.Green)
            val prog3 = state.testData.value?.progress?.cacheProgressActs?.value?.toFloat() ?: 0f
            LinearProgressIndicator({
                prog3
            }, color = Color.Yellow)
        }
    }
}
@Composable
context(_: Logger)
fun DatasetViewer2(
    state: DatasetViewerState,
    settings: DeephysSettingsController
) {
    Column {
        AsyncLoadSwapper(
            state.testData,
            nullMessage = "select a test to view it",
            fadeOutDur = DEEPHYS_FADE_DUR,
            fadeInDur = DEEPHYS_FADE_DUR
        ) {
            {
                DatasetNode(it, state, settings)
            }
        }
        disabledCode {
            BindTutorial(state)
        }
    }
}

@Suppress("unused")
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
