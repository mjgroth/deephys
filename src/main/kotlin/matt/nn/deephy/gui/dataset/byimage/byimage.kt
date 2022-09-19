package matt.nn.deephy.gui.dataset.byimage

import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.text.FontWeight.BOLD
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
import matt.nn.deephy.gui.DEEPHY_FONT
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.neuron.NeuronView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.NeuronWithActivation
import matt.nn.deephy.model.Test
import matt.nn.deephy.state.DeephyState
import matt.obs.bind.binding
import matt.obs.bindings.bool.and


class ByImageView(
  test: Test,
  viewer: DatasetViewer
): VBoxWrapper<RegionWrapper<*>>() {
  init {

	button("select random image") {
	  font = DEEPHY_FONT
	  setOnAction {
		viewer.imageSelection.value = test.resolvedImages.random()
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
		vbox {
		  spacer(10.0)
		  textflow<TextWrapper> {
			text("ground truth: ") {
			  font = DEEPHY_FONT
			}
			text(this@swapper.category) {
			  font = DEEPHY_FONT.fixed().copy(weight = BOLD).fx()
			}
		  }

		  test.model?.classificationLayer?.go {
			text("predictions:")
			val preds = this@swapper.activationsFor(it)
			val mx = preds.max()
			preds.withIndex().sortedBy { it.value }.reversed().take(5).forEach {
			  text("\t${test.category(it.index)} (${(it.value/mx).sigFigs(3)})")
			}
		  }
		}
	  }
	}.apply {
	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull)
	}

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
		  this@swapper.forEach {
			val neuron = it
			val neuronIndex = it.index
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
				if (it is NeuronWithActivation) {
				  val n = it
				  text(
					DeephyState.normalizeTopNeuronActivations.binding {
					  " (${(if (it!!) n.normalizedActivation else n.activation).sigFigs(3)})"
					}
				  ) {
					font = DEEPHY_FONT
				  }
				}
			  }

			  +NeuronView(
				viewer.model.neurons.first { it.neuron == neuron.neuron },
				numImages = DeephyState.numImagesPerNeuronInByImage,
				images = test.resolvedImages,
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

