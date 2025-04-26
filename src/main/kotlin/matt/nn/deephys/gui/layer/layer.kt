@file:Suppress("unused")

package matt.nn.deephys.gui.layer

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import matt.lang.common.go
import matt.lang.common.unsafeReturningErr
import matt.nn.deephys.gui.neuron.NeuronView
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.ResolvedLayer

@Suppress("UnusedVariable", "UNUSED_VARIABLE")
@Composable
fun LayerView(
    layer: ResolvedLayer,
    testLoader: TestLoader,
    viewer: DatasetViewerState,
    settings: DeephysSettingsController,
    viewerWidth: Dp
) {
    Column {


        val interLayer = layer.interTest

        val neurons = layer.neurons.map { it.interTest }
        val spinnerAndValue =
            unsafeReturningErr<Any>(
                """
                DeephysSpinner(
                    selected = viewer.neuronSelection,
                    label = "Neuron",
                    choices = neurons,
                    defaultChoice = { neurons[0] },
                    converter = InterTestNeuron.stringConverterThatFallsBackToFirst(neurons = neurons),
                    viewer = viewer,
                    getCurrent = viewer.neuronSelection,
                    acceptIf = { it.layer == interLayer },
                    navAction = { navigateTo(it) }
                )      
                """.trimIndent()
            )


        testLoader.postDtypeTestLoader.awaitRequireSuccessful().preppedTest.awaitSuccessfulOrNull()
            ?.go { typedTestLoader ->
                NeuronView(
                    viewer.neuronSelection.value!!,
                    testLoader = typedTestLoader,
                    viewer = viewer,
                    showActivationRatio = true,
                    layoutForList = false,
                    settings = settings,
                    showTopCats = true,
                    viewerWidth = viewerWidth
                )
            }
    }
}
