@file:Suppress("VARIABLE_NEVER_READ", "ASSIGNED_VALUE_IS_NEVER_READ")

package matt.nn.deephys.gui.dataset.byimage.neuronlistview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import matt.caching.compcache.invoke
import matt.compose.controls.desktop.scroll.MyHorizontalScrollPane
import matt.compose.graphics.text.MyText
import matt.lang.common.go
import matt.lang.common.unsafeError
import matt.lang.common.unsafeReturningErr
import matt.lang.weak.weak
import matt.math.numalg.format.sigfig.toScientificNotation
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.calc.act.ActivationRatio
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.gui.fix.withImages
import matt.nn.deephys.gui.global.DeephyActionText
import matt.nn.deephys.gui.global.SpacerWithOldFxSize
import matt.nn.deephys.gui.global.tooltip.symbol.DeephysInfoSymbol
import matt.nn.deephys.gui.neuron.NeuronView
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.load.test.PostDtypeTestLoader
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.prim.pint.ceilInt

@Composable
fun <A : Number> neuronListViewSwapper(
    viewer: DatasetViewerState,
    contents: Set<DeephyImage<A>>,
    postDtypeTestLoader: PostDtypeTestLoader<A>,
    bindScrolling: Boolean = false,
    fade: Boolean = true,
    settings: DeephysSettingsController,
    viewerWidth: Dp
) {

    val weakViewer = weak(viewer)
    NeuronListViewSwapper(
        bindScrolling = bindScrolling,
        viewer = viewer,
        fade = fade,
        top =
            derivedStateOf {
                weakViewer.deref()?.let { deRefedViewer ->
                    deRefedViewer.layerSelection.value?.let { lay ->
                        val prepped1 = postDtypeTestLoader.preppedTest
                        @Suppress("ReplaceSafeCallChainWithRun")
                        val prepped2 = deRefedViewer.normalizer.value/*.takeIf { it != deRefedViewer }*/?.testData?.value?.postDtypeTestLoader?.awaitRequireSuccessful()?.preppedTest

                        TopNeurons(
                            testAndImages = prepped1.awaitRequireSuccessful().withImages(contents),
                            layer = lay,
                            /*normalized = deRefedViewer.normalizeTopNeuronActivations.value,*/
                            denomTest = prepped2?.awaitRequireSuccessful()
                        )
                    }
                }
            },
        settings = settings,
        viewerWidth = viewerWidth
    )
}

@Suppress("UnusedParameter")
@Composable
fun NeuronListViewSwapper(
    viewer: DatasetViewerState,
    @Suppress("REDUNDANT_PROJECTION") top: State<out TopNeurons<*>?>,
    bindScrolling: Boolean = false,
    fade: Boolean = true,
    settings: DeephysSettingsController,
    viewerWidth: Dp
) {
    val weakViewer = weak(viewer)
    val b =
        derivedStateOf {
            weakViewer.deref()?.let { deRefedViewer ->
                deRefedViewer.testData.value?.let { tst ->
                    top.value?.let { topCalc ->
                        NeuronListViewConfig(
                            viewer = deRefedViewer,
                            testLoader = tst.postDtypeTestLoader.awaitRequireSuccessful().preppedTest.awaitRequireSuccessful(),
                            tops = topCalc
                        )
                    }
                }
            }
        }
    b.value?.let {
        NeuronListView(it, bindScrolling = bindScrolling, settings = settings, viewerWidth = viewerWidth)
    } ?: MyText("no top neurons. Did you select a layer and an image?")
}

data class NeuronListViewConfig(
    val viewer: DatasetViewerState,
    val tops: TopNeurons<*>,
    val testLoader: TypedTestLike<*>
)

private const val NEURON_LIST_VIEW_WIDTH = 150.0
@Suppress("LocalVariableName", "ForbiddenIsCheck")
@Composable
fun NeuronListView(
    cfg: NeuronListViewConfig,
    bindScrolling: Boolean = false,
    settings: DeephysSettingsController,
    viewerWidth: Dp
) {

    val hValueProp = rememberScrollState()
    MyHorizontalScrollPane(hValueProp) {
        Row {

            @Suppress("UNUSED_VARIABLE")
            val myHeight = 150.0
            cfg.apply {
                val weakViewer = weak(viewer)

                if (bindScrolling) {

                    viewer.currentByImageHScroll.value = hValueProp
                    val btd = viewer.boundToDSet.value
                    @Suppress("ReplaceSafeCallChainWithRun")
                    val btdScroll = btd?.currentByImageHScroll?.value
                    val btdScrollValue = btdScroll?.value
                    LaunchedEffect(btdScrollValue) {
                        btdScrollValue?.let {
                            hValueProp.scrollTo(it)
                        }
                    }
                }



                val topNeurons =
                    with(viewer.testData.value!!.testRAMCache) {
                        tops()
                    }

                val startAsyncAt = (viewerWidth.value / NEURON_LIST_VIEW_WIDTH).ceilInt().dp

                if (topNeurons.isEmpty()) {
                    DeephysInfoSymbol("There are no top neurons. This could happen if all activations are NaN, infinite, or zero.")
                }

                topNeurons.forEachIndexed { idx, neuronWithAct ->
                    val neuronIndex = neuronWithAct.neuron.index
                    Column {
                        Row {
                            DeephyActionText("neuron $neuronIndex ") {
                                val deReffedViewer = weakViewer.deref()!!
                                val viewerToChange = deReffedViewer.boundToDSet.value ?: deReffedViewer
                                viewerToChange.navigateTo(neuronWithAct.neuron)
                            }
                            with(
                                viewer.normalizer
                            ) {


                                val act = neuronWithAct.activation
                                val case_activ = cfg.tops.testAndImages.images.size
                                Row {

                                    var text = "(max:100%)"

                                    if (act is RawActivation<*, *>) text =
                                        "(max:" + act.value.toDouble().toScientificNotation(2).toString() + ")"
                                    if (act is ActivationRatio<*, *>) text =
                                        "(max:" + (act.value.toFloat() * 100).toDouble().toScientificNotation(3).toString() + "%" + ")"

                                    if (case_activ == 1) {
                                        if (act is RawActivation<*, *>) text =
                                            " Y=" + act.value.toDouble().toScientificNotation(2).toString()
                                        if (act is ActivationRatio<*, *>) text =
                                            " Y=" + (act.value.toFloat() * 100).toDouble().toScientificNotation(3).toString() + "%"
                                    }
                                    if (case_activ > 1) {
                                        if (act is RawActivation<*, *>) text =
                                            "(" + "ave:" + act.value.toDouble().toScientificNotation(2).toString() + ")"
                                        if (act is ActivationRatio<*, *>) text =
                                            "(" + "ave:" +
                                            (act.value.toFloat() * 100).toDouble().toScientificNotation(3)
                                                .toString() + "%" + ")"
                                    }

                                    unsafeError(
                                        """
                                        DeephysText(
                                            text
                                        ) {

                                            highlightOnHover()

                                            when (act) {
                                                is AlwaysOneActivation<*, *> ->
                                                    veryLazyDeephysTooltip(memSafeSettings) {
                                                        "activation is always 1 in this case, so it is not shown"
                                                    }

                                                is RawActivation<*, *>       -> {
                                                    val numImages = (cfg.tops).testAndImages.images.size
                                                    veryLazyDeephysTooltip(memSafeSettings) {
                                                        if (numImages == 0) "maximum raw activation value for this neuron"
                                                        else if (numImages > 1) "average activation value for the selected images"
                                                        else "raw activation value for the selected image"
                                                    }
                                                }

                                                is ActivationRatio<*, *>     -> {
                                                    val numImages = (cfg.tops).testAndImages.images.size
                                                    val num =
                                                        when (numImages) {
                                                            0    -> MiscActivationRatioNumerator.MAX
                                                            1    -> SingleImage(cfg.tops.testAndImages.images.first().imageID)
                                                            else -> MiscActivationRatioNumerator.IMAGE_COLLECTION
                                                        }
                                                    veryLazyDeephysTexTooltip(memSafeSettings) {
                                                        ActivationRatioCalc.latexTechnique(num)
                                                    }
                                                }
                                            }
                                        }       
                                        """.trimIndent()
                                    )

                                    act.extraInfo?.go { DeephysInfoSymbol(it) }
                                }
                            }
                        }

                        NeuronView(
                            neuronWithAct.neuron,
                            numImages = cfg.viewer.numImagesPerNeuronInByImage,
                            testLoader = testLoader,
                            viewer = viewer,
                            showActivationRatio = false,
                            layoutForList = true,
                            loadImagesAsync = idx > startAsyncAt.value,
                            settings = settings,
                            showTopCats = unsafeReturningErr("?"),
                            viewerWidth = viewerWidth
                        )

                        SpacerWithOldFxSize() /*space for the hbar*/
                        unsafeError(
                            """
                            prefWidth = NEURON_LIST_VIEW_WIDTH    
                            """.trimIndent()
                        )
                    }
                }
            }
        }
    }
}
