package matt.nn.deephy.gui.dataset.byneuron

import matt.hurricanefx.wrapper.pane.anchor.swapper.Swapper
import matt.nn.deephy.gui.layer.LayerView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.ResolvedLayer
import matt.nn.deephy.model.Test

class ByNeuronView(
  dataset: Test, viewer: DatasetViewer
): Swapper<ResolvedLayer?, LayerView>() {
  init {
	setupSwapping(viewer.layerSelection, nullMessage = "select a layer") {
	  println("making LayerView")
	  LayerView(this, dataset.resolvedImages, viewer)
	}
  }
}