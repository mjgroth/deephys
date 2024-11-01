package matt.nn.deephys.gui.dataset.byneuron

import androidx.compose.runtime.Composable
import matt.lang.common.unsafeErr
import matt.nn.deephys.gui.global.DeephysNullMessageFact
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.load.test.TestLoader

@Composable
fun ByNeuronView(
    testLoader: TestLoader,
    viewer: DatasetViewerState,
    settings: DeephysSettingsController
) {
    val memSafeSettings = settings
    val v = viewer.layerSelectionResolved.value
    v?.let {
        unsafeErr(
            """
            LayerView(it, testLoader, viewer, memSafeSettings)        
            """.trimIndent()
        )
    } ?: DeephysNullMessageFact("Select a layer to see the top images")
}


