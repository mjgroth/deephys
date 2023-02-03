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
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.lang.go
import matt.lang.weak.MyWeakRef
import matt.math.round.ceilInt
import matt.nn.deephys.calc.ActivationRatioCalc
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.MiscActivationRatioNumerator
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.SINGLE_IMAGE
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.calc.TopNeuronsCalcType
import matt.nn.deephys.calc.act.ActivationRatio
import matt.nn.deephys.calc.act.AlwaysOneActivation
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.progresspopup.withProgressPopUp
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.deephyActionText
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.tooltip.deephysInfoSymbol
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTexTooltip
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.neuron.NeuronView
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.obs.bind.MyBinding
import matt.obs.prop.ObsVal

fun <A: Number> NW.neuronListViewSwapper(
  viewer: DatasetViewer,
  contents: Contents<DeephyImage<A>>,
  bindScrolling: Boolean = false,
  fade: Boolean = true
) = run {

  val weakViewer = MyWeakRef(viewer)
  neuronListViewSwapper(
	bindScrolling = bindScrolling,
	viewer = viewer,
	fade = fade,
	top = MyBinding(
	  viewer.layerSelection, /*viewer.normalizeTopNeuronActivations, */viewer.testData, viewer.normalizer
	) {
	  weakViewer.deref()?.let { deRefedViewer ->
		deRefedViewer.layerSelection.value?.let { lay ->
		  @Suppress("UNCHECKED_CAST")
		  TopNeurons(
			images = contents,
			layer = lay,
			test = deRefedViewer.testData.value!!.preppedTest.await() as TypedTestLike<A>,
			/*normalized = deRefedViewer.normalizeTopNeuronActivations.value,*/
			denomTest = deRefedViewer.normalizer.value/*.takeIf { it != deRefedViewer }*/?.testData?.value?.preppedTest?.await() as TypedTestLike<A>?
		  )
		}
	  }
	}.apply {    /*  viewer.onGarbageCollected {
		  markInvalid()
		  removeAllDependencies()
		}*/
	}
  )
}

fun NW.neuronListViewSwapper(
  viewer: DatasetViewer, top: ObsVal<out TopNeuronsCalcType?>, bindScrolling: Boolean = false, fade: Boolean = true
) = run {
  val weakViewer = MyWeakRef(viewer)
  swapper(
	MyBinding(
	  /*viewer.normalizeTopNeuronActivations,*/ viewer.testData, top
	) {
	  weakViewer.deref()?.let { deRefedViewer ->
		deRefedViewer.testData.value?.let { tst ->
		  top.value?.let { topCalc ->
			NeuronListViewConfig(
			  viewer = deRefedViewer, testLoader = tst.preppedTest.await(), tops = topCalc
			)
		  }
		}
	  }
	},
	nullMessage = "no top neurons",
	fadeOutDur = if (fade) DEEPHYS_FADE_DUR else null,
	fadeInDur = if (fade) DEEPHYS_FADE_DUR else null
  ) {
	NeuronListView(this, bindScrolling = bindScrolling)
  }
}

data class NeuronListViewConfig(
  val viewer: DatasetViewer,
  val tops: TopNeuronsCalcType,
  val testLoader: TypedTestLike<*>
)

class NeuronListView(
  cfg: NeuronListViewConfig,
  bindScrolling: Boolean = false
): ScrollPaneWrapper<HBoxWrapperImpl<NodeWrapper>>(HBoxWrapperImpl()) {


  companion object {
	const val NEURON_LIST_VIEW_WIDTH = 150.0
  }


  init {


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


		val topNeurons = withProgressPopUp {
		  it.message = "loading tops..."
		  tops()
		}


		val startAsyncAt = (viewer.stage!!.width/NEURON_LIST_VIEW_WIDTH).ceilInt()

		if (topNeurons.isEmpty() && viewer.normalizer.value != null) {
		  deephysInfoSymbol("There are no top neurons. This could happen if all activations are NaN, infinite, or zero.")
		}

		topNeurons.forEachIndexed { idx, neuronWithAct ->
		  val neuronIndex = neuronWithAct.neuron.index
		  vbox {
			h {
			  deephyActionText("neuron $neuronIndex") {
				val deReffedViewer = weakViewer.deref()!!
				val viewerToChange = deReffedViewer.boundToDSet.value ?: deReffedViewer
				viewerToChange.navigateTo(neuronWithAct.neuron)
			  }            /*val image = if (viewer.isBoundToDSet.value) null else viewer.imageSelection.value*/


			  spacer(10.0)

			  /*val normalize = viewer.normalizeTopNeuronActivations*/
			  swapperRNullable(
				viewer.normalizer
				/*.binding(*//*normalize*//*) { it }*/
			  ) {                /*val inD = it*/                /*if (inD == null || inD == viewer) {*/


				/*anirban and xavier asked to hide the activation text in this case since it doesn't pertain to a specific image*/
				if (neuronWithAct.activation !is ActivationRatio || (cfg.tops as TopNeurons<*>).images.isNotEmpty()) {
				  val act = neuronWithAct.activation
				  h {
					deephyText(
					  act.formatted
					) {

					  highlightOnHover()

					  when (act) {
						is AlwaysOneActivation -> veryLazyDeephysTooltip { "activation is always 1 in this case, so it is not shown" }
						is RawActivation       -> {
						  val numImages = (cfg.tops as TopNeurons<*>).images.size
						  if (numImages > 1) {
							veryLazyDeephysTooltip {
							  "average activation value for the selected images"
							}
						  } else {
							veryLazyDeephysTooltip {
							  "raw activation value for the selected image"
							}
						  }

						}

						is ActivationRatio     -> {
						  /*val forced = (cfg.tops as TopNeurons<*>).forcedNeuronIndices*/
						  val numImages = (cfg.tops as TopNeurons<*>).images.size
						  val num = when (numImages) {
							0 -> MiscActivationRatioNumerator.MAX
							1 -> SINGLE_IMAGE(cfg.tops.images.first().imageID)
							else -> MiscActivationRatioNumerator.IMAGE_COLLECTION
						  }
						  veryLazyDeephysTexTooltip {
							ActivationRatioCalc.latexTechnique(num)
						  }
						}
						/*ActivationRatioCalc.technique*/
						/*is NormalActivation    -> NormalizedAverageActivation.normalizeTopNeuronsBlurb*/

					  }


					}
					//					infoSymbol("test")
					act.extraInfo?.go { deephysInfoSymbol(it) }
				  }
				}


				/*} else inD.testData.value?.let { inDTest ->
				  deephyText(
					ActivationRatioCalc(
					  numTest = testLoader,
					  denomTest = inDTest,
					  neuron = neuronWithAct.neuron
					)().formatted
				  ) {
					deephyTooltip(ActivationRatioCalc.technique)
				  }*/                /*}*/ /*?: deephyText("error: no activation info")*/
			  }


			}
			+NeuronView(
			  neuronWithAct.neuron,
			  numImages = cfg.viewer.numImagesPerNeuronInByImage,
			  testLoader = testLoader,
			  viewer = viewer,
			  showActivationRatio = false,
			  layoutForList = true,
			  loadImagesAsync = idx > startAsyncAt
			)

			spacer() /*space for the hbar*/
			prefWidth = NEURON_LIST_VIEW_WIDTH
		  }
		}
	  }
	}
  }
}