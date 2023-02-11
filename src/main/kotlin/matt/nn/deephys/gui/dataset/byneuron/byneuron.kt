package matt.nn.deephys.gui.dataset.byneuron

import matt.fx.graphics.wrapper.pane.anchor.swapper.Swapper
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.deephysNullMessageFact
import matt.nn.deephys.gui.layer.LayerView
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.importformat.testlike.TypedTestLike

class ByNeuronView(
  testLoader: TypedTestLike<*>, viewer: DatasetViewer, override val settings: DeephysSettingsController
): Swapper<ResolvedLayer?, LayerView>(), DeephysNode {
  init {
	val memSafeSettings = settings
	nullNodeFact v deephysNullMessageFact
	setupSwapping(
	  viewer.layerSelectionResolved,
	  nullMessage = "Select a layer to see the top images",
	  fadeOutDur = DEEPHYS_FADE_DUR,
	  fadeInDur = DEEPHYS_FADE_DUR
	) {
	  LayerView(this, testLoader, viewer, memSafeSettings)
	}
  }
}


