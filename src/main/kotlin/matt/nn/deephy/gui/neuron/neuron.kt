package matt.nn.deephy.gui.neuron

import javafx.scene.layout.Priority
import matt.hurricanefx.wrapper.canvas.CanvasWrapper
import matt.hurricanefx.wrapper.pane.flow.FlowPaneWrapper
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.NeuronLike
import matt.nn.deephy.model.ResolvedDeephyImage
import kotlin.math.min

class NeuronView(
  neuron: NeuronLike,
  numImages: Int = 100,
  images: List<ResolvedDeephyImage>,
  viewer: DatasetViewer,

  ): FlowPaneWrapper<CanvasWrapper>() {
  init {
	val realNumImages = min(numImages, images.size)
	(0 until realNumImages).forEach { imIndex ->
	  val im = images[neuron.activationIndexesHighToLow[imIndex]]
	  +DeephyImView(im, viewer)
	  vgrow = Priority.ALWAYS
	}
  }
}

