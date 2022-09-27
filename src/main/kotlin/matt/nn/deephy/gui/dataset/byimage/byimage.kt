package matt.nn.deephy.gui.dataset.byimage

import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.text.Font
import javafx.scene.text.FontWeight.BOLD
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.mtofx.createROFXPropWrapper
import matt.hurricanefx.font.fixed
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.PaneWrapper
import matt.hurricanefx.wrapper.pane.hbox.HBoxWrapper
import matt.hurricanefx.wrapper.pane.scroll.ScrollPaneWrapper
import matt.hurricanefx.wrapper.pane.spacer
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.hurricanefx.wrapper.text.TextWrapper
import matt.lang.go
import matt.math.jmath.sigFigs
import matt.math.sumOf
import matt.nn.deephy.calc.ActivationRatio
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.global.deephyActionText
import matt.nn.deephy.gui.global.deephyButton
import matt.nn.deephy.gui.global.deephyText
import matt.nn.deephy.gui.global.deephyTooltip
import matt.nn.deephy.gui.neuron.NeuronView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.NeuronWithActivation
import matt.nn.deephy.model.normalizeTopNeuronsBlurb
import matt.nn.deephy.state.DeephySettings
import matt.obs.bind.binding
import matt.obs.bindings.bool.and
import matt.prim.str.truncateWithElipsesOrAddSpaces
import kotlin.math.exp


class ByImageView(
  testLoader: TestLoader, viewer: DatasetViewer
): VBoxWrapper<RegionWrapper<*>>() {
  init {
	deephyButton("select random image") {
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
			val groundTruthFont: Font.()->Font = { fixed().copy(size = size*2).fx() }
			deephyText("ground truth: ") {
			  font = font.groundTruthFont()
			}
			deephyText(this@swapper.category) {
			  font = font.groundTruthFont().fixed().copy(weight = BOLD).fx()
			}
		  }
		  spacer()
		  testLoader.model.classificationLayer?.go { lay ->
			text("predictions:")
			val preds = this@swapper.activationsFor(lay)
			val softMaxDenom = preds.sumOf { exp(it) }
			val predNamesBox: NodeWrapper = vbox<TextWrapper> {}
			val predValuesBox: NodeWrapper = vbox<TextWrapper> {}
			hbox<PaneWrapper<*>> {
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
			  predNamesBox.deephyText(predClassNameString.truncateWithElipsesOrAddSpaces(25)) {
				deephyTooltip(fullString)
			  }
			  predValuesBox.deephyText {
				textProperty.bind(DeephySettings.predictionSigFigs.binding {
				  exactPred.sigFigs(it).toString()
				})
				deephyTooltip(fullString)
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
				deephyActionText("neuron $neuronIndex") {
				  val viewerToChange = viewer.boundToDSet.value ?: viewer
				  viewerToChange.neuronSelection.value = null
				  viewerToChange.layerSelection.value = neuron.layer
				  viewerToChange.neuronSelection.value =
					viewerToChange.model.neurons.first { it.neuron == neuron.neuron }
				  viewerToChange.view.value = ByNeuron
				}
				val normSett = DeephySettings.normalizeTopNeuronActivations
				if (neuron is NeuronWithActivation) {
				  deephyText(normSett.binding {
					" ${if (it) "Y" else "Ŷ"}=${
					  (if (it) neuron.normalizedActivation else neuron.activation).sigFigs(
						3
					  )
					}"
				  }) {
					deephyTooltip(normalizeTopNeuronsBlurb) {
					  textProperty().bind(normSett.binding {
						if (it) normalizeTopNeuronsBlurb else "raw activation value for the selected image"
					  }.createROFXPropWrapper())
					}
				  }
				} else {
				  deephyText(
					ActivationRatio(
					  numTest = testLoader,
					  denomTest = viewer.boundToDSet.value!!.testData.value!!,
					  neuron = neuron.interTest
					).formattedResult
				  ) {
					deephyTooltip(ActivationRatio.technique)
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

