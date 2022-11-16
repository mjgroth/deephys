package matt.nn.deephys.gui.neuron

import matt.lang.weak.WeakPair
import matt.nn.deephys.calc.TopImages
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.data.InterTestNeuron
import matt.obs.math.double.op.times
import matt.obs.prop.BindableProperty
import kotlin.math.min

class NeuronView(
  neuron: InterTestNeuron,
  numImages: BindableProperty<Int> = BindableProperty(100),
  testLoader: TestLoader,
  viewer: DatasetViewer,

  ): ImageFlowPane(viewer) {
  init {

	/*for reasons that I don't understand, without this this FlowPane gets really over-sized in the y dimension*/
	prefWrapLengthProperty.bind(viewer.widthProperty*0.8)

	fun update(testLoaderAndViewer: Pair<TestLoader, DatasetViewer>) {
	  val localTestLoader = testLoaderAndViewer.first
	  val localViewer = testLoaderAndViewer.second
	  clear()
	  val realNumImages = min(numImages.value.toULong(), localTestLoader.numImages.await())
	  val topImages = TopImages(neuron, localTestLoader, realNumImages.toInt())()


	  topImages.forEach {
		val im = localTestLoader.awaitImage(it.index)
		+DeephyImView(im, localViewer)
	  }
	}
	update(testLoader to viewer)
	numImages.onChangeWithAlreadyWeak(WeakPair(testLoader, viewer)) { tl, _ ->
	  update(tl)
	}
  }
}

