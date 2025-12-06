@file:Suppress("UnusedParameter")

package matt.nn.deephys.gui.neuron

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import matt.async.thread.queue.QueueWorker
import matt.caching.compcache.invoke
import matt.codegen.tex.tex
import matt.collect.itr.subList
import matt.compose.state.produce.produceSimpleResettingIoState
import matt.compose.state.shortcuts.rememberMutableStateOf
import matt.lang.common.go
import matt.lang.common.unsafeError
import matt.lang.function.Consume
import matt.math.lang.arithmetic.op.div
import matt.model.code.successorfail.resultwithval.loadedOrNull
import matt.model.flowlogic.await.Donable
import matt.nn.deephys.calc.ActivationRatioCalc
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.MiscActivationRatioNumerator.MAX
import matt.nn.deephys.calc.TopCategories
import matt.nn.deephys.calc.TopImages
import matt.nn.deephys.calc.act.ActivationRatio
import matt.nn.deephys.calc.act.AlwaysOneActivation
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.gui.dataset.byimage.preds.CategoryTable
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.global.tooltip.symbol.DeephysInfoSymbol
import matt.nn.deephys.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.settings.MAX_NUM_IMAGES_IN_TOP_IMAGES
import matt.nn.deephys.gui.unsafemigration.ImageFlowPane
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.model.data.ImageIndex
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.reflect.weak.WeakThing
import kotlin.math.min

private val worker = QueueWorker("NeuronView Worker")

@Suppress("UnusedVariable", "UNUSED_VARIABLE")
@Composable
fun <A : Number> NeuronView(
    neuron: InterTestNeuron,
    numImages: State<Int> = rememberMutableStateOf(MAX_NUM_IMAGES_IN_TOP_IMAGES),
    testLoader: TypedTestLike<A>,
    viewer: DatasetViewerState,
    showActivationRatio: Boolean,
    layoutForList: Boolean,
    loadImagesAsync: Boolean = false,
    showTopCats: Boolean = false,
    settings: DeephysSettingsController,
    viewerWidth: Dp
) {

    Column {

        val weakViewer = viewer.weakRef
        val showing = rememberMutableStateOf(2)
        val showProgIndicator = showing.value < 2
        if (showProgIndicator) {
            CircularProgressIndicator()
        }

        if (showActivationRatio) {
            with(viewer.normalizer.value) {

                val normalizer = this
                weakViewer.deref()!!.testData.value?.go { numTest ->
                    val denomTest = normalizer?.run { testData.value }
                    Row {
                        val doneLoading = numTest.isDoneLoading() && (denomTest?.isDoneLoading() != false)

                        if (!doneLoading) showing.value -= 1

                        val producedActivation =
                            produceSimpleResettingIoState {
                                denomTest?.let {
                                    with(viewer.cacheContext) {
                                        neuron.activationRatio(
                                            numTest = numTest.postDtypeTestLoader.awaitRequireSuccessful().preppedTest.awaitRequireSuccessful(),
                                            denomTest = denomTest.postDtypeTestLoader.awaitRequireSuccessful().preppedTest.awaitRequireSuccessful()
                                        )
                                    }
                                } ?: neuron.maxActivationIn(
                                    test = numTest.postDtypeTestLoader.awaitRequireSuccessful().preppedTest.awaitRequireSuccessful() as TypedTestLike<*>
                                )
                            }

                        producedActivation.value.loadedOrNull()?.go { activation ->
                            if (!doneLoading) {
                                showing.value += 1
                            }
                            DeephysTooltipArea(
                                settings = settings,
                                getCode = {
                                    when (activation) {
                                        is ActivationRatio     -> ActivationRatioCalc.latexTechnique(MAX)
                                        is RawActivation       -> tex { text("max raw activation of this neuron") }
                                        is AlwaysOneActivation -> ActivationRatioCalc.latexTechnique(MAX)
                                    }
                                },
                                content = {
                                    DeephysText(
                                        s =    activation.formatted
                                    )
                                }
                            )

                            activation.extraInfo?.go { DeephysInfoSymbol(it) }
                        }
                    }
                }
            }
        }

        if (showTopCats) {
            val topCats = with(testLoader.testRAMCache) { TopCategories(neuron, testLoader)() }

            val dtype = testLoader.dtype

            with(viewer.normalizer.value) {
                val normalizer =
                    this?.run { testData.value?.run { postDtypeTestLoader.awaitRequireSuccessful().preppedTest.awaitRequireSuccessful() } }
                val denom =
                    normalizer?.let {
                        neuron.maxActivationIn(normalizer).value / 100
                    } ?: dtype.one

                val normalizedString = if (normalizer == null) "un-normalized" else "normalized"

                CategoryTable(
                    title = "Average activity for top categories: ",
                    titleUnfolded = "ave: ",
                    data = topCats.map { it.first to (it.second.value / denom) },
                    settings = settings,
                    weakViewer = weakViewer,
                    sigFigSett = weakViewer.deref()!!.averageRawActSigFigs,
                    tooltip = "Top categories for this neuron. Calculated by the average, $normalizedString activation of this neuron for images grouped by their groundtruth",
                    numSuffix = if (normalizer == null) "" else "%"
                )
            }

            Spacer(Modifier.size(1.0.dp))
        }

        val noneText =
            DeephysInfoSymbol(
                "There are no top images. This might happen if all activations are zero, NaN, or infinite"
            )
        ImageFlowPane(
            viewer,
            /*for reasons that I don't understand, without this FlowPane gets really over-sized in the y dimension*/
            prefWrapLengthProperty = (viewerWidth.value * 0.95).dp
        ) {
            unsafeError(
                """
                noneText.visibleAndManagedProp.bindWeakly(
                    children.sizeProperty.eq(0) and !showProgIndicator
                )    
                """.trimIndent()
            )

            val imFlowPane = this
            fun update(
                weakThing: WeakNeuronViewRefs<A>,
                oldNumImages: Int?,
                newNumImages: Int
            ) {
                val localTestLoader = weakThing.testLoader
                val localViewer = weakThing.viewer
                val localNeuron = weakThing.neuron
                val localImFlowPane = weakThing.imFlowPane

                val realOldNumImages =
                    oldNumImages?.let { min(it.toULong(), localTestLoader.numberOfImages()) }
                val realNumImages = min(newNumImages.toULong(), localTestLoader.numberOfImages())

                val doneLoading = localTestLoader.isDoneLoading()

                val topImagesJob =
                    with(viewer.cacheContext) {
                        if (localTestLoader.isDoneLoading()) {
                            val ti =
                                with(localTestLoader.testRAMCache) {
                                    TopImages(
                                        localNeuron,
                                        localTestLoader,
                                        realNumImages.toInt()
                                    )()
                                }
                            object : Donable<List<ImageIndex>> {
                                override fun whenDone(c: Consume<List<ImageIndex>>) {
                                    c(ti)
                                }
                            }
                        } else {
                            showing.value -= 1
                            worker.schedule {
                                with(localTestLoader.testRAMCache) {
                                    TopImages(
                                        localNeuron,
                                        localTestLoader,
                                        realNumImages.toInt()
                                    )()
                                }
                            }
                        }
                    }
                topImagesJob.whenDone { topImages ->
                    when (realOldNumImages) {
                        null                                      -> {
                            topImages.forEach {
                                val im = localTestLoader.imageAtIndex(it.index)
                                unsafeError(
                                    """
                                    localImFlowPane.add(
                                        DeephyImView(
                                            im,
                                            localViewer,
                                            loadAsync = loadImagesAsync,
                                            settings = memSafeSettings
                                        )
                                    )       
                                    """.trimIndent()
                                )
                            }
                        }

                        else if realNumImages > realOldNumImages  -> {
                            topImages.subList(realOldNumImages.toInt()).toList().forEach {
                                val im = localTestLoader.imageAtIndex(it.index)
                                unsafeError(
                                    """
                                    localImFlowPane.add(
                                        DeephyImView(
                                            im,
                                            localViewer,
                                            loadAsync = loadImagesAsync,
                                            settings = memSafeSettings
                                        )
                                    )             
                                    """.trimIndent()
                                )
                            }
                        }

                        else if  realNumImages < realOldNumImages -> {
                            unsafeError(
                                """
                                localImFlowPane.children.subList(realNumImages.toInt()).toList().forEach {
                                    it.removeFromParent()
                                }         
                                """.trimIndent()
                            )
                        }
                    }
                    if (!doneLoading) {
                        showing.value += 1
                    }
                }
            }

            val weakThing =
                WeakNeuronViewRefs<A>().apply {
                    this.testLoader = testLoader
                    this.viewer = viewer
                    this.neuron = neuron
                    this.imFlowPane = imFlowPane
                }

            update(weakThing.deref()!!, null, numImages.value)

            unsafeError(
                """
                numImages.onChangeWithAlreadyWeakAndOld(weakThing) { tl, o, n ->
                    update(tl, o, n)
                }
                if (layoutForList) {
                    prefWrapLength = NeuronListView.NEURON_LIST_VIEW_WIDTH
                }
                val gap = 3.0
                hgap = gap
                vgap = gap           
                """.trimIndent()
            )
        }
    }
}

private class WeakNeuronViewRefs<A : Number> : WeakThing<WeakNeuronViewRefs<A>>() {

    override fun constructNew(): WeakNeuronViewRefs<A> = WeakNeuronViewRefs()

    var testLoader by weak<TypedTestLike<A>>()
    var viewer by weak<DatasetViewerState>()
    var neuron by weak<InterTestNeuron>()
    var imFlowPane by weak<ImageFlowPane>()
}
