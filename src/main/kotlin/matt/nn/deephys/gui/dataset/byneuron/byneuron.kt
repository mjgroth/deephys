package matt.nn.deephys.gui.dataset.byneuron

import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.pane.anchor.swapper.Swapper
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.lang.weak.WeakRefInter
import matt.nn.deephys.gui.dataset.MainDeephysView
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.deephysNullMessageFact
import matt.nn.deephys.gui.layer.LayerView
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.ResolvedLayer
import matt.obs.bind.binding
import matt.obs.prop.ObsVal

class ByNeuronView(
    testLoader: TestLoader,
    viewer: DatasetViewer,
    override val settings: DeephysSettingsController
): Swapper<ResolvedLayer?, NW>(), MainDeephysView {

    override val control: ObsVal<WeakRefInter<RegionWrapper<*>>?> by lazy {
        children.binding {
            if (it.isEmpty()) null else (it.filterIsInstance<LayerView>().firstOrNull()?.spinnerThing)
        }
    }

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


