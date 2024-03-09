package matt.nn.deephys.gui.layer

import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperNeverNull
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.lang.common.go
import matt.lang.weak.common.WeakRefInter
import matt.lang.weak.weak
import matt.nn.deephys.gui.global.deephysSpinner
import matt.nn.deephys.gui.neuron.NeuronView
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.data.InterTestNeuron

class LayerView(
    layer: ResolvedLayer,
    testLoader: TestLoader,
    viewer: DatasetViewer,
    override val settings: DeephysSettingsController
): VBoxWrapperImpl<RegionWrapper<*>>(), DeephysNode {


    var spinnerThing: WeakRefInter<RegionWrapper<*>>? = null
        private set

    init {

        val memSafeSettings = settings

        val interLayer = layer.interTest

        val neurons = layer.neurons.map { it.interTest }
        val spinnerAndValue =
            deephysSpinner(
                label = "Neuron",
                choices = neurons,
                defaultChoice = { neurons[0] },
                converter = InterTestNeuron.stringConverterThatFallsBackToFirst(neurons = neurons),
                viewer = viewer,
                getCurrent = viewer.neuronSelection,
                acceptIf = { it.layer == interLayer },
                navAction = { navigateTo(it) }
            ).apply {
                this@LayerView.spinnerThing = weak(first)
            }

        testLoader.preppedTest.awaitSuccessfulOrNull()?.go { typedTestLoader ->
            swapperNeverNull(spinnerAndValue.second) {
                NeuronView(
                    this,
                    testLoader = typedTestLoader,
                    viewer = viewer,
                    showActivationRatio = true,
                    layoutForList = false,
                    settings = memSafeSettings,
                    showTopCats = true
                )
            }
        }
    }
}
