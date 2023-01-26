package matt.nn.deephys.gui.neuron

import matt.collect.set.contents.contentsOf
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperR
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.node.proto.infosymbol.infoSymbol
import matt.lang.go
import matt.lang.weak.WeakPair
import matt.lang.weak.WeakRef
import matt.nn.deephys.calc.ActivationRatioCalc
import matt.nn.deephys.calc.TopImages
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.NeuronListView
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.obs.math.double.op.times
import matt.obs.prop.BindableProperty
import kotlin.math.min

class NeuronView<A: Number>(
  neuron: InterTestNeuron,
  numImages: BindableProperty<Int> = BindableProperty(100),
  testLoader: TypedTestLike<A>,
  viewer: DatasetViewer,
  showActivationRatio: Boolean,
  layoutForList: Boolean
): VBoxWrapperImpl<NW>() {
  init {
	val weakViewer = WeakRef(viewer)
	if (showActivationRatio) {
	  swapperR(viewer.inD) {
		weakViewer.deref()!!.testData.value?.go { numTest ->
		  it.testData.value?.go { denomTest ->
			h {
			  @Suppress("UNCHECKED_CAST")
			  val ratio = ActivationRatioCalc(
				numTest = numTest.preppedTest.await() as TypedTestLike<A>,
				images = contentsOf(),
				denomTest = denomTest.preppedTest.await() as TypedTestLike<A>,
				neuron = neuron
			  )()
			  deephyText(
				ratio.formatted
			  ) {
				deephyTooltip(ActivationRatioCalc.technique)
			  }
			  ratio.extraInfo?.go { infoSymbol(it) }
			}
		  }
		}
	  }
	}
	+ImageFlowPane(viewer).apply {
	  /*for reasons that I don't understand, without this this FlowPane gets really over-sized in the y dimension*/
	  prefWrapLengthProperty.bind(viewer.widthProperty*0.8)

	  fun update(testLoaderAndViewer: Pair<TypedTestLike<A>, DatasetViewer>) {
		val localTestLoader = testLoaderAndViewer.first
		val localViewer = testLoaderAndViewer.second
		clear()
		val realNumImages = min(numImages.value.toULong(), localTestLoader.numberOfImages())
		val topImages = TopImages(neuron, localTestLoader, realNumImages.toInt())()


		topImages.forEach {
		  val im = localTestLoader.imageAtIndex(it.index)
		  +DeephyImView(im, localViewer)
		}
	  }
	  update(testLoader to viewer)
	  numImages.onChangeWithAlreadyWeak(WeakPair(testLoader, viewer)) { tl, _ ->
		update(tl)
	  }
	  if (layoutForList) {
		prefWrapLength = NeuronListView.NEURON_LIST_VIEW_WIDTH
		hgap = 10.0
		vgap = 10.0
	  }
	}
  }
}

