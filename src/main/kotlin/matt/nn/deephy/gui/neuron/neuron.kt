package matt.nn.deephy.gui.neuron

import javafx.scene.layout.Priority
import matt.hurricanefx.wrapper.canvas.CanvasWrapper
import matt.hurricanefx.wrapper.pane.flow.FlowPaneWrapper
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.NeuronTestResults
import matt.nn.deephy.model.ResolvedNeuron
import matt.obs.prop.BindableProperty
import kotlin.math.min

class NeuronView(
  neuron: ResolvedNeuron,
  numImages: BindableProperty<Int?> = BindableProperty(100),
  testLoader: TestLoader,
  viewer: DatasetViewer,

  ): FlowPaneWrapper<CanvasWrapper>() {
  init {

	fun update() {
	  clear()
	  val realNumImages = min(numImages.value!!, testLoader.numImages.await())

	  val testResults = NeuronTestResults(
		neuron.neuron,
		testLoader.awaitFinishedTest().images.map { it.activations.activations.await()[neuron.layer.index][neuron.index] }
	  )

	  (0 until realNumImages).forEach { imIndex ->
		//		println("imIndex=${imIndex}")
		//		println("another index= ${testResults.activationIndexesHighToLow[imIndex]}")
		val im = testLoader.awaitImage(testResults.activationIndexesHighToLow[imIndex])
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

