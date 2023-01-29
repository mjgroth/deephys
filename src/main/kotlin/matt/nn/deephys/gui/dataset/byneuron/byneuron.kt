package matt.nn.deephys.gui.dataset.byneuron

import matt.fx.graphics.wrapper.pane.anchor.swapper.Swapper
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.layer.LayerView
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.importformat.testlike.TypedTestLike

class ByNeuronView(
  testLoader: TypedTestLike<*>, viewer: DatasetViewer
): Swapper<ResolvedLayer?, LayerView>() {
  init {
	setupSwapping(
	  viewer.layerSelectionResolved,
	  nullMessage = "no layer selected",
	  fadeOutDur = DEEPHYS_FADE_DUR,
	  fadeInDur = DEEPHYS_FADE_DUR
	) {
	  println("making LayerView")
	  LayerView(this, testLoader, viewer)
	}
  }
}