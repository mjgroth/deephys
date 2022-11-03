package matt.nn.deephys.gui.dataset.byimage.neuronlistview

import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import matt.fx.control.wrapper.scroll.ScrollPaneWrapper
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.hbox.HBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.fx.graphics.wrapper.textflow.textflow
import matt.lang.go
import matt.nn.deephys.calc.ActivationRatio
import matt.nn.deephys.calc.NormalizedAverageActivation
import matt.nn.deephys.calc.TopNeurons
import matt.nn.deephys.calc.TopNeuronsCalcType
import matt.nn.deephys.calc.UniqueContents
import matt.nn.deephys.calc.act.NormalActivation
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.gui.global.deephyActionText
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.neuron.NeuronView
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.importformat.DeephyImage
import matt.nn.deephys.state.DeephySettings
import matt.obs.bind.MyBinding
import matt.obs.prop.ObsVal

fun NW.neuronListViewSwapper(
  viewer: DatasetViewer,
  contents: UniqueContents<DeephyImage>
) = neuronListViewSwapper(
  viewer = viewer,
  top = MyBinding(
	viewer.layerSelection,
	DeephySettings.normalizeTopNeuronActivations
  ) {
	viewer.layerSelection.value?.let { lay ->
	  TopNeurons(
		images = contents,
		layer = lay,
		normalized = DeephySettings.normalizeTopNeuronActivations.value
	  )
	}
  }
)

fun NW.neuronListViewSwapper(
  viewer: DatasetViewer, top: ObsVal<out TopNeuronsCalcType?>
) = swapper(
  MyBinding(
	DeephySettings.normalizeTopNeuronActivations, viewer.testData, top
  ) {
	viewer.testData.value?.let { tst ->
	  top.value?.let { topCalc ->
		NeuronListViewConfig(
		  viewer = viewer,
		  testLoader = tst,
		  tops = topCalc
		)
	  }
	}
  }, "no top neurons"
) {
  NeuronListView(this)
}


data class NeuronListViewConfig(
  val viewer: DatasetViewer,
  val tops: TopNeuronsCalcType,
  val testLoader: TestLoader
)

class NeuronListView(
  cfg: NeuronListViewConfig
): ScrollPaneWrapper<HBoxWrapperImpl<NodeWrapper>>(HBoxWrapperImpl()) {




  init {
	hbarPolicy = AS_NEEDED
	vbarPolicy = AS_NEEDED
	isFitToHeight = true
	vmax = 0.0


	val myWidth = 150.0
	@Suppress("UNUSED_VARIABLE") val myHeight = 150.0
	cfg.apply {
	  viewer.currentByImageHScroll = hValueProp
	  viewer.boundToDSet.value?.currentByImageHScroll?.value?.go { hvalue = it }
	  hValueProp.onChange { h ->
		if (viewer.outerBox.bound.value != null) {
		  viewer.siblings.forEach { it.currentByImageHScroll?.value = h }
		}
	  }
	  viewer.boundToDSet.onChange {
		viewer.boundToDSet.value?.currentByImageHScroll?.value?.go { hvalue = it }
	  }

	  content!!.apply {
		tops().forEach { neuronWithAct ->
		  val neuronIndex = neuronWithAct.neuron.index
		  vbox {
			textflow<TextWrapper> {
			  deephyActionText("neuron $neuronIndex") {
				val viewerToChange = viewer.boundToDSet.value ?: viewer
				viewerToChange.navigateTo(neuronWithAct.neuron)
			  }
			  val image = if (viewer.isBoundToDSet.value) null else viewer.imageSelection.value


			  if (image != null) {


				deephyText(neuronWithAct.activation.formatted) {
				  deephyTooltip(
					when (neuronWithAct.activation) {
					  is RawActivation -> "raw activation value for the selected image"
					  is NormalActivation -> NormalizedAverageActivation.normalizeTopNeuronsBlurb
					}
				  )
				}


			  } else +(viewer.boundToDSet.value?.testData?.value?.let {
				ActivationRatio(
				  numTest = testLoader,
				  denomTest = it,
				  neuron = neuronWithAct.neuron
				).text()
			  } ?: deephyText("error: no activation info"))


			}
			+NeuronView(
			  neuronWithAct.neuron, numImages = DeephySettings.numImagesPerNeuronInByImage, testLoader = testLoader,
			  viewer = viewer
			).apply {
			  prefWrapLength = myWidth
			  hgap = 10.0
			  vgap = 10.0
			}

			spacer() /*space for the hbar*/
			prefWidth = myWidth
		  }
		}
	  }
	}
  }
}