package matt.nn.deephys.gui.dataset.byimage.neuronlistview

import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import matt.fx.control.wrapper.scroll.ScrollPaneWrapper
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.HBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.fx.graphics.wrapper.textflow.textflow
import matt.hurricanefx.eye.lib.onChange
import matt.lang.go
import matt.nn.deephys.calc.ActivationRatio
import matt.nn.deephys.calc.NormalizedAverageActivation
import matt.nn.deephys.calc.TopNeuronsCalcType
import matt.nn.deephys.gui.global.deephyActionText
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.neuron.NeuronView
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.state.DeephySettings

class NeuronListView(
  viewer: DatasetViewer,
  tops: TopNeuronsCalcType,
  normalized: Boolean,
  testLoader: TestLoader
): ScrollPaneWrapper<HBoxWrapperImpl<NodeWrapper>>(HBoxWrapperImpl()) {
  init{
	hbarPolicy = AS_NEEDED
	vbarPolicy = AS_NEEDED
	isFitToHeight = true
	vmax = 0.0


	val myWidth = 150.0
	@Suppress("UNUSED_VARIABLE") val myHeight = 150.0

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

	content.apply {
	  tops().forEach { neuronWithAct ->
		val neuronIndex = neuronWithAct.neuron.index
		vbox {
		  textflow<TextWrapper> {
			deephyActionText("neuron $neuronIndex") {
			  val viewerToChange = viewer.boundToDSet.value ?: viewer
			  viewerToChange.navigateTo(neuronWithAct.neuron)
			}
			val image = viewer.imageSelection.value
			if (image != null) {

		/*	  val symbol = if (normalized) NORMALIZED_ACT_SYMBOL else RAW_ACT_SYMBOL
			  val value = if (normalized) NormalizedActivation(
				neuron, image, testLoader.awaitFinishedTest()
			  )() else neuron.activation(image)*/
			  val blurb =
				if (normalized) NormalizedAverageActivation.normalizeTopNeuronsBlurb else "raw activation value for the selected image"

			  deephyText(neuronWithAct.activation.formatted) {
				deephyTooltip(blurb)
			  }
			} else +ActivationRatio(
			  numTest = testLoader,
			  denomTest = viewer.boundToDSet.value!!.testData.value!!,
			  neuron = neuronWithAct.neuron
			).text()
		  }
		  +NeuronView(
			neuronWithAct.neuron,
			numImages = DeephySettings.numImagesPerNeuronInByImage,
			testLoader = testLoader,
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