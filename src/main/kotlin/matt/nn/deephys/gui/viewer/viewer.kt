@file:Suppress("UNUSED_VARIABLE")

package matt.nn.deephys.gui.viewer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import matt.caching.compcache.ComputeCacheContext
import matt.caching.compcache.findOrCompute
import matt.collect.itr.filterNotNull
import matt.collect.weak.lazy.lazyWeakMap
import matt.compose.controls.collapse.CollapsePane
import matt.compose.controls.dialog.filechoose.RealComposeFileChooser
import matt.compose.graphics.layout.AlignedRow
import matt.compose.snap.setToNull
import matt.compose.snap.withSafeMutableSnapshot
import matt.compose.state.shortcuts.rememberMutableStateOf
import matt.file.construct.mFile
import matt.file.ext.FileExtension
import matt.file.ext.singleExtensionOrNullIfNoDots
import matt.file.model.file.types.Cbor
import matt.file.model.file.types.TypedFile
import matt.file.types.checkType
import matt.lang.anno.optin.UnsafeMattCode
import matt.lang.assertions.require.requireNot
import matt.lang.common.DoNothing
import matt.lang.common.disabledCode
import matt.lang.common.unsafeError
import matt.lang.common.unsafeReturningErr
import matt.lang.weak.weak
import matt.log.profile.stopwatch.stopwatch
import matt.log.profile.stopwatch.tic
import matt.log.warn.common.warn
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.gui.dataset.DatasetNode
import matt.nn.deephys.gui.dataset.DatasetNodeView
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
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
import matt.obs.bind.deepBinding
import matt.obs.bind.deepBindingIgnoringFutureNullOuterChanges
import matt.obs.bind.weakBinding
import matt.obs.bindings.bool.not
import matt.obs.bindings.comp.gt
import matt.obs.bindings.comp.lt
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.col.olist.lastIndexProperty
import matt.obs.prop.ObsVal
import matt.obs.prop.withChangeListener
import matt.obs.prop.writable.BindableProperty
import matt.obs.prop.writable.VarProp
import matt.obs.prop.writable.withNonNullUpdatesFrom
import matt.prim.common.exportfromlang.model.file.MacFileSystem
import matt.prim.common.exportfromlang.model.file.fName
import matt.prim.str.mybuild.api.string

class DatasetViewerState(
    initialFile: TypedFile<Cbor, *>? = null,
    val outerBox: DSetViewsState,
    settings: DeephysSettingsController,
    val cacheContext: ComputeCacheContext
) {
    val showAsList1 = BindableProperty(false)
    val showAsList2 = BindableProperty(false)
    val model by lazy {  unsafeError("outerBox.model") }
    @Suppress("unused")
    val siblings by lazy {
        unsafeError(
            """
            outerBox.datasets.filtered { it != this }        
            """.trimIndent()
        )
    }
    @Suppress("unused")
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
    private val isBoundToDSet by lazy { boundToDSet.isNotNull }
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
        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
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

    @Suppress("unused")
    private val boundNeuron: ObsVal<Neuron?> =
        boundToDSet.deepBindingIgnoringFutureNullOuterChanges {
            unsafeReturningErr {
                it?.neuronSelection
            }
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

    private val boundTopNeurons: MyBinding<TopNeurons<*>?> =
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

    private val topNeurons: MyBinding<TopNeurons<*>?> = boundTopNeurons coalesceNull unsafeReturningErr { topNeuronsFromMyImage }

    @Suppress("unused")
    val highlightedNeurons =
        derivedStateOf {
            when (view.value) {
                ByNeuron   -> arrayOf(neuronSelection.value).filterNotNull()

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
                it?.run { catSelectionForViewer[weakRef.deref()!!] } ?: BindableProperty(null)
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
        neuronSelection.setToNull()
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
                    val cats = testData.value?.run { test.categories }
                    if (cats?.isNotEmpty() == true) {
                        categorySelection.value = cats.first()
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
        view.value = theView
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
        this@DatasetViewerState.history.binding(historyIndex, boundToDSet) {
            isUnboundToDSet.value && it.isNotEmpty()
        }
}

@OptIn(UnsafeMattCode::class)
@Suppress("UnusedParameter")
@Composable
fun DatasetViewer(
    state: DatasetViewerState,
    settings: DeephysSettingsController
) {
    /*titleProperty.bind(file.binding { it?.nameWithoutExtension })*/
    CollapsePane(
        title = {
            AlignedRow(horizontalArrangement = Arrangement.Center) {
                SectionSpacer()
                @Suppress("unused")
                val removeTestButton =
                    DeephysTooltipArea(settings, "remove this test viewer") {
                        DeephyIconButton("icon/minus") {
                            state.outerBox.removeTest(state)
                        }
                    }

                val showFileChooser = rememberMutableStateOf(false)
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
                                state.file.value = (mFile(f.path, MacFileSystem)).checkType(Cbor)
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
                    enabled =
                        state.canUseHistory.value and
                            state.historyIndex.lt(
                                state.history.lastIndexProperty
                            ).value
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

                DeephysText(
                    state.testData.binding {
                        it?.run { testName.awaitSuccessfulOrMessage().toString() } ?: "please select a test"
                    },
                    style = titleFont()
                )
                Spacer(Modifier.width(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(DEEPHYS_SYMBOL_SPACING.dp)) {
                    DeephysInfoSymbol(

                        state.testData.binding {
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

                    )

                    val loadWarnings = state.testData.value!!.loadWarnings

                    Row(horizontalArrangement = Arrangement.spacedBy(DEEPHYS_SYMBOL_SPACING.dp)) {
                        loadWarnings.forEach {
                            DeephysWarningSymbol(it)
                        }
                    }
                }

                SectionSpacer()

                val prog1 =
                    state.testData.deepBinding {
                        it?.run { progress.progress } ?: BindableProperty(0.0)
                    }.value.toFloat()
                if (prog1 < 1.0) {
                    LinearProgressIndicator({
                        prog1
                    })
                }

                if (state.showCacheBars.value) {
                    val prog2 =
                        state.testData.deepBinding {
                            it?.run { progress.cacheProgressPixels } ?: BindableProperty(0.0)
                        }.value.toFloat()
                    LinearProgressIndicator({
                        prog2
                    }, color = Color.Green)
                    val prog3 =
                        state.testData.deepBinding {
                            it?.run { progress.cacheProgressActs } ?: BindableProperty(0.0)
                        }.value.toFloat()
                    LinearProgressIndicator({
                        prog3
                    }, color = Color.Yellow)
                }
            }
        },
        expanded = rememberMutableStateOf(true)
    ) {
        Column {
            AsyncLoadSwapper(
                state.testData.binding { it },
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
