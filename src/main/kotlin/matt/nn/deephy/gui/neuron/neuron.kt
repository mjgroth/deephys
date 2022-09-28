package matt.nn.deephy.gui.neuron

import matt.nn.deephy.calc.TopImages
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.data.InterTestNeuron
import matt.obs.bindings.math.times
import matt.obs.prop.BindableProperty
import kotlin.math.min

class NeuronView(
  neuron: InterTestNeuron,
  numImages: BindableProperty<Int> = BindableProperty(100),
  testLoader: TestLoader,
  viewer: DatasetViewer,

  ): ImageFlowPane(viewer) {
  init {

	/*for reasons I don't understand, without this this FlowPane gets really over-sized in the y dimension*/
	prefWrapLengthProperty.bind(viewer.widthProperty*0.8)

	fun update() {
	  clear()
	  val realNumImages = min(numImages.value.toULong(), testLoader.numImages.await())
	  val topImages = TopImages(neuron, testLoader)()


	  (0.toULong() until realNumImages).forEach { imIndex ->
		require(imIndex <= Int.MAX_VALUE.toULong())
		val im = testLoader.awaitImage(topImages[imIndex.toInt()].index)
		+DeephyImView(im, viewer)
	  }
	}
	update()
	numImages.onChange {
	  update()
	}
  }
}

