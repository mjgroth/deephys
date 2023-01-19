package matt.nn.deephys.gui.dataset.byneuron

import matt.fx.graphics.wrapper.pane.anchor.swapper.Swapper
import matt.nn.deephys.gui.layer.LayerView
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.ResolvedLayer

class ByNeuronView(
  testLoader: TestLoader, viewer: DatasetViewer
): Swapper<ResolvedLayer?, LayerView>() {
  init {
	setupSwapping(
	  viewer.layerSelectionResolved,
	  nullMessage = "no layer selected"
	) {
	  println("making LayerView")
	  LayerView(this, testLoader, viewer)
	}
  }
}