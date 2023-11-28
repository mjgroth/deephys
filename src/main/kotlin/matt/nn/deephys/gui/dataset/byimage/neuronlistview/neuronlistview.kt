package matt.nn.deephys.gui.dataset.byimage.neuronlistview

import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import matt.collect.set.contents.Contents
import matt.fx.control.wrapper.scroll.ScrollPaneWrapper
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperRNullable
import matt.fx.graphics.wrapper.pane.hbox.HBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.fx.graphics.wrapper.text.textlike.highlightOnHover
import matt.lang.go
import matt.lang.weak.MyWeakRef
import matt.prim.int.ceilInt
import matt.math.numalg.format.sigfig.toScientificNotation
import matt.nn.deephys.calc.ActivationRatioCalc
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.MiscActivationRatioNumerator
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.SINGLE_IMAGE
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.calc.TopNeuronsCalcType
import matt.nn.deephys.calc.act.ActivationRatio
import matt.nn.deephys.calc.act.AlwaysOneActivation
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.deephyActionText
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.tooltip.symbol.deephysInfoSymbol
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTexTooltip
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.neuron.NeuronView
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.obs.bind.MyBinding
import matt.obs.prop.ObsVal

fun <A : Number> NW.neuronListViewSwapper(
    viewer: DatasetViewer,
    contents: Contents<DeephyImage<A>>,
    bindScrolling: Boolean = false,
    fade: Boolean = true,
    settings: DeephysSettingsController
) = run {

    val weakViewer = MyWeakRef(viewer)
    neuronListViewSwapper(
        bindScrolling = bindScrolling,
        viewer = viewer,
        fade = fade,
        top = MyBinding(
            viewer.layerSelection,
            /*viewer.normalizeTopNeuronActivations,
            * */viewer.testData,
            viewer.normalizer
        ) {
            weakViewer.deref()?.let { deRefedViewer ->
                deRefedViewer.layerSelection.value?.let { lay ->
                    @Suppress("UNCHECKED_CAST")
                    TopNeurons(
                        images = contents,
                        layer = lay,
                        test = deRefedViewer.testData.value!!.preppedTest.awaitRequireSuccessful() as TypedTestLike<A>,
                        /*normalized = deRefedViewer.normalizeTopNeuronActivations.value,*/
                        denomTest = deRefedViewer.normalizer.value/*.takeIf { it != deRefedViewer }*/?.testData?.value?.preppedTest?.awaitRequireSuccessful() as TypedTestLike<A>?
                    )
                }
            }
        }.apply {

            /*  viewer.onGarbageCollected {
                markInvalid()
                removeAllDependencies()
              }*/

        },
        settings = settings
    )
}

fun NW.neuronListViewSwapper(
    viewer: DatasetViewer,
    top: ObsVal<out TopNeuronsCalcType?>,
    bindScrolling: Boolean = false,
    fade: Boolean = true,
    settings: DeephysSettingsController
) = run {
    val weakViewer = MyWeakRef(viewer)
    swapper(
        MyBinding(
            viewer.testData,
            top
        ) {
            weakViewer.deref()?.let { deRefedViewer ->
                deRefedViewer.testData.value?.let { tst ->
                    top.value?.let { topCalc ->
                        NeuronListViewConfig(
                            viewer = deRefedViewer,
                            testLoader = tst.preppedTest.awaitRequireSuccessful(),
                            tops = topCalc
                        )
                    }
                }
            }
        },
        nullMessage = "no top neurons. Did you select a layer and an image?",
        fadeOutDur = if (fade) DEEPHYS_FADE_DUR else null,
        fadeInDur = if (fade) DEEPHYS_FADE_DUR else null
    ) {
        NeuronListView(this, bindScrolling = bindScrolling, settings = settings)
    }
}

data class NeuronListViewConfig(
    val viewer: DatasetViewer,
    val tops: TopNeuronsCalcType,
    val testLoader: TypedTestLike<*>
)

class NeuronListView(
    cfg: NeuronListViewConfig,
    bindScrolling: Boolean = false,
    override val settings: DeephysSettingsController
) : ScrollPaneWrapper<HBoxWrapperImpl<NodeWrapper>>(HBoxWrapperImpl()), DeephysNode {


    companion object {
        const val NEURON_LIST_VIEW_WIDTH = 150.0
    }


    init {

        val memSafeSettings = settings

        val weakThisNLV = MyWeakRef(this)

        hbarPolicy = AS_NEEDED
        vbarPolicy = AS_NEEDED
        isFitToHeight = true
        vmax = 0.0


        @Suppress("UNUSED_VARIABLE") val myHeight = 150.0
        cfg.apply {
            val weakViewer = MyWeakRef(viewer)

            if (bindScrolling) {

                viewer.currentByImageHScroll = hValueProp
                viewer.boundToDSet.value?.currentByImageHScroll?.value?.go { hvalue = it }
                hValueProp.onChangeWithAlreadyWeak(weakViewer) { vw, h ->
                    if (vw.outerBox.bound.value != null) {
                        vw.siblings.forEach { it.currentByImageHScroll?.value = h }
                    }
                }
                viewer.boundToDSet.onChangeWithAlreadyWeak(weakViewer) { vw, _ ->
                    vw.boundToDSet.value?.currentByImageHScroll?.value?.go { weakThisNLV.deref()?.hvalue = it }
                }
            }


            content!!.apply {

                val topNeurons = with(viewer.testData.value!!.testRAMCache) {
                    tops()
                }


                /*val topNeurons = withProgressPopUp {
                  it.message = "loading tops..."
                  tops()
                }*/


                val startAsyncAt = (viewer.width / NEURON_LIST_VIEW_WIDTH).ceilInt()

                if (topNeurons.isEmpty()) {
                    deephysInfoSymbol("There are no top neurons. This could happen if all activations are NaN, infinite, or zero.")
                }

                topNeurons.forEachIndexed { idx, neuronWithAct ->
                    val neuronIndex = neuronWithAct.neuron.index
                    v {
                        h {
                            deephyActionText("neuron $neuronIndex ") {
                                val deReffedViewer = weakViewer.deref()!!
                                val viewerToChange = deReffedViewer.boundToDSet.value ?: deReffedViewer
                                viewerToChange.navigateTo(neuronWithAct.neuron)
                            }
                            spacer(.0)
                            swapperRNullable(
                                viewer.normalizer
                            ) {


                                if (
                                    true
                                /*neuronWithAct.activation !is ActivationRatio
                                || (cfg.tops as TopNeurons<*>).images.isNotEmpty()*/
                                ) {
                                    val act = neuronWithAct.activation
                                    val case_activ = (cfg.tops as TopNeurons<*>).images.size
                                    h {

                                        var text = "(max:100%)"

                                        if (act is RawActivation) text = "(max:" + act.value.toDouble().toScientificNotation(2).toString() + ")"
                                        if (act is ActivationRatio) text =
                                            "(max:" + (act.value.toFloat() * 100).toDouble().toScientificNotation(3).toString() + "%" + ")"

                                        if (case_activ == 1) {
                                            if (act is RawActivation) text = " Y=" + act.value.toDouble().toScientificNotation(2).toString()
                                            if (act is ActivationRatio) text =
                                                " Y=" + (act.value.toFloat() * 100).toDouble().toScientificNotation(3).toString() + "%"
                                        }
                                        if (case_activ > 1) {
                                            if (act is RawActivation) text =
                                                "(" + "ave:" + act.value.toDouble().toScientificNotation(2).toString() + ")"
                                            if (act is ActivationRatio) text =
                                                "(" + "ave:" + (act.value.toFloat() * 100).toDouble().toScientificNotation(3)
                                                    .toString() + "%" + ")"
                                        }

                                        deephysText(
                                            text
                                        ) {

                                            highlightOnHover()

                                            when (act) {
                                                is AlwaysOneActivation -> veryLazyDeephysTooltip(memSafeSettings) { "activation is always 1 in this case, so it is not shown" }
                                                is RawActivation       -> {
                                                    val numImages = (cfg.tops).images.size
                                                    veryLazyDeephysTooltip(memSafeSettings) {
                                                        if (numImages == 0) "maximum raw activation value for this neuron"
                                                        else if (numImages > 1) "average activation value for the selected images"
                                                        else "raw activation value for the selected image"
                                                    }

                                                }

                                                is ActivationRatio     -> {
                                                    val numImages = (cfg.tops).images.size
                                                    val num = when (numImages) {
                                                        0 -> MiscActivationRatioNumerator.MAX
                                                        1 -> SINGLE_IMAGE(cfg.tops.images.first().imageID)
                                                        else -> MiscActivationRatioNumerator.IMAGE_COLLECTION
                                                    }
                                                    veryLazyDeephysTexTooltip(memSafeSettings) {
                                                        ActivationRatioCalc.latexTechnique(num)
                                                    }
                                                }
                                            }
                                        }
                                        act.extraInfo?.go { deephysInfoSymbol(it) }
                                    }
                                }
                            }


                        }

                        +NeuronView(
                            neuronWithAct.neuron,
                            numImages = cfg.viewer.numImagesPerNeuronInByImage,
                            testLoader = testLoader,
                            viewer = viewer,
                            showActivationRatio = false,
                            layoutForList = true,
                            loadImagesAsync = idx > startAsyncAt,
                            settings = memSafeSettings
                        )

                        spacer() /*space for the hbar*/
                        prefWidth = NEURON_LIST_VIEW_WIDTH
                    }
                }
            }
        }
    }
}