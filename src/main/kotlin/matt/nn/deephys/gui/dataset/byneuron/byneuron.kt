package matt.nn.deephys.gui.dataset.byneuron

import androidx.compose.runtime.Composable
import matt.lang.common.unsafeError
import matt.nn.deephys.gui.global.DeephysNullMessageFact
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.load.test.TestLoader

@Suppress("UnusedParameter")
@Composable
fun ByNeuronView(
    testLoader: TestLoader,
    viewer: DatasetViewerState,
    settings: DeephysSettingsController
) {
    val v = viewer.layerSelectionResolved.value
    v?.let {
        unsafeError(
            """
            LayerView(it, testLoader, viewer, memSafeSettings)        
            """.trimIndent()
        )
    } ?: DeephysNullMessageFact("Select a layer to see the top images")
}
