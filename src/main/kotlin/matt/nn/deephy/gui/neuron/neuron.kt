package matt.nn.deephy.gui.neuron

import javafx.scene.layout.Priority
import matt.hurricanefx.wrapper.canvas.CanvasWrapper
import matt.hurricanefx.wrapper.pane.flow.FlowPaneWrapper
import matt.nn.deephy.gui.DatasetViewer
import matt.nn.deephy.gui.dataset.DatasetNode
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.model.DeephyImage
import matt.nn.deephy.model.Neuron
import kotlin.math.min

class NeuronView(
  neuron: Neuron,
  numImages: Int = 100,
  images: List<DeephyImage>,
  viewer: DatasetViewer,
  dsNode: DatasetNode,

  ): FlowPaneWrapper<CanvasWrapper>() {
  init {
	val realNumImages = min(numImages, images.size)
	(0 until realNumImages).forEach { imIndex ->
	  val im = images[neuron.activationIndexesHighToLow[imIndex]]
	  +DeephyImView(im, viewer, dsNode)
	  vgrow = Priority.ALWAYS
	}
  }
}

