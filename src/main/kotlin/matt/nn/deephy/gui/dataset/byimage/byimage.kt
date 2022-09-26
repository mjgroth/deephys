package matt.nn.deephy.gui.dataset.byimage

import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.text.FontWeight.BOLD
import javafx.util.Duration
import matt.fx.graphics.node.actionText
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.font.fixed
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.hbox.HBoxWrapper
import matt.hurricanefx.wrapper.pane.scroll.ScrollPaneWrapper
import matt.hurricanefx.wrapper.pane.spacer
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.hurricanefx.wrapper.text.TextWrapper
import matt.lang.go
import matt.math.jmath.sigFigs
import matt.math.sumOf
import matt.nn.deephy.gui.DEEPHY_FONT
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.neuron.NeuronView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.NeuronWithActivation
import matt.nn.deephy.model.ResolvedNeuron
import matt.nn.deephy.state.DeephySettings
import matt.obs.bind.binding
import matt.obs.bindings.bool.and
import matt.prim.str.truncateWithElipsesOrAddSpaces
import kotlin.math.exp


class ByImageView(
  testLoader: TestLoader, viewer: DatasetViewer
): VBoxWrapper<RegionWrapper<*>>() {
  init {

	button("select random image") {
	  font = DEEPHY_FONT
	  setOnAction {

		viewer.imageSelection.value = testLoader.awaitNonUniformRandomImage()
	  }
	  visibleAndManagedProp.bind(
		viewer.imageSelection.isNull.and(viewer.topNeurons.isNull)
	  )
	}

	swapper(viewer.imageSelection, "no image selected") {
	  HBoxWrapper<NodeWrapper>().apply {
		+DeephyImView(this@swapper, viewer).apply {
		  scale.value = 4.0
		}
		spacer(10.0)
		vbox {

		  textflow<TextWrapper> {
			val groundTruthFont = DEEPHY_FONT.fixed().copy(size = DEEPHY_FONT.size*2).fx()
			text("ground truth: ") {
			  font = groundTruthFont
			}
			text(this@swapper.category) {
			  font = groundTruthFont.fixed().copy(weight = BOLD).fx()
			}
		  }
		  spacer()
		  testLoader.model.classificationLayer?.go { lay ->
			text("predictions:")
			val preds = this@swapper.activationsFor(lay)
			val softMaxDenom = preds.sumOf { exp(it) }
			val predNamesBox: NodeWrapper = vbox<TextWrapper> {}
			val predValuesBox: NodeWrapper = vbox<TextWrapper> {}
			hbox<NodeWrapper> {
			  +predNamesBox
			  spacer()
			  +predValuesBox
			}
			preds.withIndex().sortedBy { it.value }.reversed().take(5).forEach { thePred ->
			  val exactPred = (exp(thePred.value)/softMaxDenom)
			  val predClassNameString = testLoader.category(thePred.index).let {
				if (", texture :" in it) {
				  it.substringBefore(",")
				} else it
			  }
			  val fullString = "\t${predClassNameString} (${exactPred})"
			  predNamesBox.text(predClassNameString.truncateWithElipsesOrAddSpaces(25)) {
				font = DEEPHY_FONT
				tooltip(fullString) {
				  showDelay = Duration.millis(100.0)
				  hideDelay = Duration.millis(1000.0)
				}
			  }
			  predValuesBox.text("${exactPred.sigFigs(DeephySettings.predictionSigFigs.value)}") {
				font = DEEPHY_FONT
				tooltip(fullString) {
				  showDelay = Duration.millis(100.0)
				  hideDelay = Duration.millis(1000.0)
				}
			  }
			}
		  }
		}
	  }
	}.apply {
	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull)
	}
	spacer(10.0)
	swapper(viewer.topNeurons, "no top neurons") {

	  ScrollPaneWrapper<HBoxWrapper<NodeWrapper>>().apply {
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


		content = HBoxWrapper<NodeWrapper>().apply {
		  this@swapper.forEach { neuron ->
			val neuronIndex = neuron.index
			vbox {
			  textflow<TextWrapper> {
				actionText("neuron $neuronIndex") {

				  val viewerToChange = viewer.boundToDSet.value ?: viewer
				  viewerToChange.neuronSelection.value = null
				  viewerToChange.layerSelection.value = neuron.layer
				  viewerToChange.neuronSelection.value =
					viewerToChange.model.neurons.first { it.neuron == neuron.neuron }
				  viewerToChange.view.value = ByNeuron
				}.apply {
				  font = DEEPHY_FONT
				}
				if (neuron is NeuronWithActivation) {
				  text(DeephySettings.normalizeTopNeuronActivations.binding {
					" (${(if (it) neuron.normalizedActivation else neuron.activation).sigFigs(3)})"
				  }) {
					font = DEEPHY_FONT
				  }
				} else {
				  val theNeuron = (neuron as ResolvedNeuron)
				  val boundTo = viewer.boundToDSet.value!!
				  val boundNeuron =
					(boundTo.topNeurons.value!!.first { it.index == theNeuron.index } as NeuronWithActivation).rNeuron
				  val activationRatio =
					testLoader.awaitFinishedTest().maxActivations[theNeuron]/boundTo.testData.value!!.awaitFinishedTest().maxActivations[boundNeuron]
				  text(" (${activationRatio.sigFigs(3)})") {
					font = DEEPHY_FONT
				  }
				}
			  }

			  +NeuronView(
				viewer.model.neurons.first { it.neuron == neuron.neuron },
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
  }
}

