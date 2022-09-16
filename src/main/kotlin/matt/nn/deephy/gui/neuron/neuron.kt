package matt.nn.deephy.gui.neuron

import javafx.scene.layout.Priority
import matt.hurricanefx.wrapper.canvas.CanvasWrapper
import matt.hurricanefx.wrapper.pane.flow.FlowPaneWrapper
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.NeuronTestResults
import matt.nn.deephy.model.ResolvedDeephyImage
import matt.nn.deephy.model.ResolvedNeuron
import matt.obs.prop.BindableProperty
import kotlin.math.min

class NeuronView(
  neuron: ResolvedNeuron,
  numImages: BindableProperty<Int?> = BindableProperty(100),
  images: List<ResolvedDeephyImage>,
  viewer: DatasetViewer,

  ): FlowPaneWrapper<CanvasWrapper>() {
  init {

	fun update() {
	  clear()
	  val realNumImages = min(numImages.value!!, images.size)
	  println("images.size=${images.size}")

	  val testResults =
		NeuronTestResults(neuron.neuron, images.map { it.activations.activations[neuron.layer.index][neuron.index] })

	  (0 until realNumImages).forEach { imIndex ->
//		println("imIndex=${imIndex}")
//		println("another index= ${testResults.activationIndexesHighToLow[imIndex]}")
		val im = images[testResults.activationIndexesHighToLow[imIndex]]
		+DeephyImView(im, viewer)
		vgrow = Priority.ALWAYS
	  }
	}
	update()
	numImages.onChange {
	  update()
	}


  }
}

