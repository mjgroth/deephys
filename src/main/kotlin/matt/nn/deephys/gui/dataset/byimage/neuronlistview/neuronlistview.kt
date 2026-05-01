@file:Suppress("VARIABLE_NEVER_READ", "ASSIGNED_VALUE_IS_NEVER_READ")

package matt.nn.deephys.gui.dataset.byimage.neuronlistview

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import matt.caching.compcache.invoke
import matt.compose.controls.desktop.scroll.MyHorizontalScrollPane
import matt.compose.controls.interaction.rememberMutableInteractionSource
import matt.compose.graphics.defaults.TextDefaults
import matt.compose.graphics.text.MyText
import matt.lang.controlflow.go
import matt.lang.err.unsafeReturningErr
import matt.math.numalg.format.sigfig.toScientificNotation
import matt.model.k.log.Logger
import matt.nn.deephys.calc.ActivationRatioCalc
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.MiscActivationRatioNumerator
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.SingleImage
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.calc.act.ActivationRatio
import matt.nn.deephys.calc.act.AlwaysOneActivation
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.gui.fix.withImages
import matt.nn.deephys.gui.global.DeephyActionText
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.SpacerWithOldFxSize
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.global.tooltip.symbol.DeephysInfoSymbol
import matt.nn.deephys.gui.neuron.NeuronView
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.load.test.PostDtypeTestLoader
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.prim.pint.ceilInt
import matt.prim.weak.weak

@Composable
context(_: Logger)
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
                        val prepped2 = deRefedViewer.normalizer.value/*.takeIf { it != deRefedViewer }*/?.run { testData.value?.run { this.postDtypeTestLoader.awaitRequireSuccessful().preppedTest } }

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
context(_: Logger)
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

const val NEURON_LIST_VIEW_WIDTH = 150.0
@Suppress("LocalVariableName", "ForbiddenIsCheck", "D")
@Composable
context(_: Logger)
fun NeuronListView(
    cfg: NeuronListViewConfig,
    bindScrolling: Boolean = false,
    settings: DeephysSettingsController,
    viewerWidth: Dp
) {

    val hValueProp = rememberScrollState()
    MyHorizontalScrollPane(horizontalScrollState = hValueProp) {
        Row {

            @Suppress("UNUSED_VARIABLE")
            val myHeight = 150.0
            cfg.apply {
                val weakViewer = weak(viewer)

                if (bindScrolling) {

                    viewer.currentByImageHScroll.value = hValueProp
                    val btd = viewer.boundToDSet.value
                    val btdScroll = btd?.run { currentByImageHScroll.value }
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
                    Column(Modifier.width(NEURON_LIST_VIEW_WIDTH.dp)) {
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

                                    val interactionSource = rememberMutableInteractionSource()

                                    val theText =
                                        remember {
                                            movableContentOf {
                                                DeephysText(
                                                    text,
                                                    Modifier
                                                        .hoverable(interactionSource),
                                                    color =
                                                        animateColorAsState(
                                                            if (
                                                                interactionSource.collectIsHoveredAsState().value
                                                            ) MaterialTheme.colorScheme.primary else TextDefaults.Color
                                                        ).value

                                                )
                                            }
                                        }

                                    when (act) {
                                        is AlwaysOneActivation<*, *> ->
                                            DeephysTooltipArea(
                                                settings,
                                                "activation is always 1 in this case, so it is not shown"

                                            ) {
                                                theText()
                                            }

                                        is RawActivation<*, *>       -> {
                                            val numImages = (cfg.tops).testAndImages.images.size

                                            DeephysTooltipArea(
                                                settings,
                                                s =
                                                    if (numImages == 0) "maximum raw activation value for this neuron"
                                                    else if (numImages > 1) "average activation value for the selected images"
                                                    else "raw activation value for the selected image"
                                            ) {
                                                theText()
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
                                            DeephysTooltipArea(
                                                settings,
                                                { ActivationRatioCalc.latexTechnique(num) }
                                            ) {
                                                theText()
                                            }
                                        }
                                    }

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
                    }
                }
            }
        }
    }
}
