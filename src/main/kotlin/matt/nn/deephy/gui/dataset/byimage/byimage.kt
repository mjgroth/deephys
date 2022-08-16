package matt.nn.deephy.gui.dataset.byimage

import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.paint.Color
import matt.fx.graphics.style.DarkModeController
import matt.hurricanefx.eye.wrapper.obs.obsval.prop.toNullableProp
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.node.onHover
import matt.hurricanefx.wrapper.node.onLeftClick
import matt.hurricanefx.wrapper.pane.hbox.HBoxWrapper
import matt.hurricanefx.wrapper.pane.spacer
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.nn.deephy.gui.DatasetViewer
import matt.nn.deephy.gui.dataset.DatasetNode
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.neuron.NeuronView
import matt.nn.deephy.model.Dataset
import matt.nn.deephy.model.Neuron

class TopNeuron(
  val neuron: Neuron,
  val index: Int,
  val activation: Double
)

class ByImageView(
  dataset: Dataset,
  viewer: DatasetViewer,
  dsNode: DatasetNode
): VBoxWrapper<RegionWrapper<*>>() {
  init {
	swapper(viewer.imageSelection.toNullableProp(), "no image selected") {
	  VBoxWrapper<NodeWrapper>().apply {
		+DeephyImView(this@swapper, viewer, dsNode).apply {
		  scale.value = 4.0
		}
		val imIndex = dataset.images.indexOf(this@swapper)
		val topNeurons = dataset
		  .layers
		  .flatMap { it.neurons }
		  .withIndex()
		  .toList()
		  .map {
			TopNeuron(
			  neuron = it.value,
			  index = it.index,
			  activation = it.value.activations[imIndex]
			)
		  }
		  .sortedBy { it.activation }
		  .reversed()
		scrollpane(HBoxWrapper<NodeWrapper>()) {
		  hbarPolicy = AS_NEEDED
		  vbarPolicy = AS_NEEDED
		  isFitToHeight = true

		  val size = 150.0

		  prefHeight = size

		  content.apply {
			topNeurons.take(25).forEach {
			  val neuron = it.neuron
			  val neuronIndex = it.index
			  vbox {
				text("neuron $neuronIndex") {
				  onHover {
					fill = when {
					  it                                    -> Color.YELLOW
					  DarkModeController.darkModeProp.value -> Color.WHITE
					  else                                  -> Color.BLACK
					}
				  }
				  onLeftClick {
					if (viewer.bound.value) {
					  viewer.outerBox.myToggleGroup.selectToggle(null)
					}
					viewer.neuronSelection.value = null
					viewer.layerSelection.value = dataset.layers.first { neuron in it.neurons }
					viewer.neuronSelection.value = IndexedValue(index = neuronIndex, value = neuron)
					dsNode.view.value = ByNeuron
				  }
				}
				+NeuronView(
				  neuron, numImages = 9, images = dataset.images, viewer = viewer, dsNode = dsNode
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
}