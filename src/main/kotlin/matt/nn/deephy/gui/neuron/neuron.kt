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
  numImages: BindableProperty<Int> = BindableProperty(100),
  testLoader: TestLoader,
  viewer: DatasetViewer,

  ): FlowPaneWrapper<CanvasWrapper>() {
  init {

	fun update() {
	  //	  val t = tic("neuron view update")
	  //	  t.toc(1)
	  clear()
	  //	  t.toc(2)
	  val realNumImages = min(numImages.value.toULong(), testLoader.numImages.await())
	  //	  t.toc(3)
	  val testResults = NeuronTestResults(
		neuron.neuron,
		testLoader.awaitFinishedTest().activationsByNeuron[neuron]
		//		testLoader.awaitFinishedTest().images.map { it.activations.activations.await()[neuron.layer.index][neuron.index] }
	  )
	  //	  t.toc(4)
//	  println("realNumImages=$realNumImages")
	  (0.toULong() until realNumImages).forEach { imIndex ->
		require(imIndex <= Int.MAX_VALUE.toULong())
		val im = testLoader.awaitImage(testResults.activationIndexesHighToLow[imIndex.toInt()])
		+DeephyImView(im, viewer)
		vgrow = Priority.ALWAYS
	  }
	  //	  t.toc(5)
	}
	update()
	numImages.onChange {
	  update()
	}


  }
}

