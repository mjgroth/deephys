package matt.nn.deephy.gui.dataset.byimage

import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.paint.Color
import matt.fx.graphics.style.DarkModeController
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.wrapper.obs.obsval.prop.toNullableProp
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.node.onHover
import matt.hurricanefx.wrapper.node.onLeftClick
import matt.hurricanefx.wrapper.pane.hbox.HBoxWrapper
import matt.hurricanefx.wrapper.pane.scroll.ScrollPaneWrapper
import matt.hurricanefx.wrapper.pane.spacer
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.hurricanefx.wrapper.target.EventTargetWrapper
import matt.hurricanefx.wrapper.text.TextWrapper
import matt.kjlib.jmath.sigFigs
import matt.klib.lang.go
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.neuron.NeuronView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.Dataset
import matt.nn.deephy.model.NeuronWithActivation


class ByImageView(
  dataset: Dataset,
  viewer: DatasetViewer
): VBoxWrapper<RegionWrapper<*>>() {
  init {

	button("select random image") {
	  setOnAction {
		viewer.imageSelection.value = dataset.resolvedImages.random()
	  }
	  visibleAndManagedProp().bind(
		viewer.imageSelection.isNull.and(viewer.topNeurons.isNull)
	  )
	}

	swapper(viewer.imageSelection.toNullableProp(), "no image selected") {
	  VBoxWrapper<NodeWrapper>().apply {
		+DeephyImView(this@swapper, viewer).apply {
		  scale.value = 4.0
		}
	  }
	}.apply {
	  visibleAndManagedProp().bind(viewer.boundTo.isNull)
	}

	swapper(viewer.topNeurons.toNullableProp(), "no top neurons") {
	  ScrollPaneWrapper<HBoxWrapper<NodeWrapper>>().apply {
		hbarPolicy = AS_NEEDED
		vbarPolicy = AS_NEEDED
		isFitToHeight = true

		val size = 150.0

		prefHeight = size


		viewer.currentByImageHScroll = hValueProp
		viewer.boundTo.value?.currentByImageHScroll?.value?.go { hvalue = it }
		hValueProp.onChange { h ->
		  if (viewer.outerBox.bound.value != null) {
			viewer.siblings.forEach { it.currentByImageHScroll?.value = h }
		  }
		}
		viewer.boundTo.onChange {
		  viewer.boundTo.value?.currentByImageHScroll?.value?.go { hvalue = it }
		}



		content = HBoxWrapper<NodeWrapper>().apply {
		  this@swapper.forEach {
			val neuron = it
			val neuronIndex = it.index
			vbox {
			  textflow<TextWrapper> {
				actionText("neuron $neuronIndex") {
				  val viewerToChange = viewer.boundTo.value ?: viewer
				  viewerToChange.neuronSelection.value = null
				  viewerToChange.layerSelection.value = neuron.layer
				  viewerToChange.neuronSelection.value = neuron
				  viewerToChange.view.value = ByNeuron
				}
				if (it is NeuronWithActivation) {
				  text(" (${it.activation.sigFigs(3)})")
				}
			  }

			  +NeuronView(
				neuron,
				numImages = 9,
				images = dataset.resolvedImages,
				viewer = viewer
			  ).apply {
				prefWrapLength = size
				hgap = 10.0
				vgap = 10.0
			  }
			  spacer() /*space for the hbar*/
			  prefWidth = size
			}
		  }
		}
	  }
	}
  }
}

fun EventTargetWrapper.actionText(text: String, action: ()->Unit) = text(text) {
  onHover {
	fill = when {
	  it                                    -> Color.YELLOW
	  DarkModeController.darkModeProp.value -> Color.WHITE
	  else                                  -> Color.BLACK
	}
  }
  onLeftClick {
	action()
  }
}