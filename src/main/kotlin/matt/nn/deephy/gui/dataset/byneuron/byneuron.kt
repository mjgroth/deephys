package matt.nn.deephy.gui.dataset.byneuron

import matt.hurricanefx.wrapper.pane.anchor.swapper.Swapper
import matt.nn.deephy.gui.layer.LayerView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.ResolvedLayer

class ByNeuronView(
  testLoader: TestLoader, viewer: DatasetViewer
): Swapper<ResolvedLayer?, LayerView>() {
  init {
	setupSwapping(viewer.layerSelection, nullMessage = "select a layer") {
	  println("making LayerView")
	  LayerView(this, testLoader, viewer)
	}
  }
}