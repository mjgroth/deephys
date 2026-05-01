@file:Suppress("UnusedVariable", "UNUSED_VARIABLE")

package matt.nn.deephys.gui.visbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import matt.compose.graphics.defaults.StandardFonts
import matt.compose.graphics.layout.observeDpHeight
import matt.compose.state.shortcuts.rememberMutableStateOf
import matt.lang.codecomment.disabledCode
import matt.lang.controlflow.go
import matt.lang.err.unsafeError
import matt.lang.err.unsafeReturningErr
import matt.model.k.log.Logger
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.DeephyButton
import matt.nn.deephys.gui.global.DeephyCheckbox
import matt.nn.deephys.gui.global.DeephyIconButton
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.global.tooltip.SUFFIX_WARNING
import matt.nn.deephys.gui.global.tooltip.symbol.DEEPHYS_SYMBOL_SPACING
import matt.nn.deephys.gui.global.tooltip.symbol.DeephysInfoSymbol
import matt.nn.deephys.gui.global.tooltip.symbol.DeephysWarningSymbol
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.modelvis.ModelVisualizerState
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.load.CborSyncLoadResult
import matt.nn.deephys.load.LoadSwapper
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
import matt.obs.prop.writable.BindableProperty
import matt.obs.prop.writable.v
import matt.osi.serfile.AbsLinuxFile
import matt.prim.str.mybuild.api.string
import matt.prim.str.truncateWithEllipsesOrAddSpaces

@Suppress("UnusedParameter", "unused")
@Composable
context(_: Logger)
fun VisBox(
    app: DeephysApp,
    settings: DeephysSettingsController,
    loadedModel: State<CborSyncLoadResult<Model>?>,
    state: DeephyState,
    dsetViews: DSetViewsState,
    showSettingsWindow: () -> Unit,
    modelVisualizerState: ModelVisualizerState
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val prefButtonHeight = rememberMutableStateOf(25.0.dp)
        val pleaseLoadModelToSeeVisualizerText = "Choose a model in order to visualize it"
        val visualizerToolTipText = BindableProperty(pleaseLoadModelToSeeVisualizerText)
        val showVisualizer = rememberMutableStateOf(false)

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            DeephyButton(
                s = "Select Model",
                modifier = Modifier.height(prefButtonHeight.value)
            ) {
                val f =
                    unsafeReturningErr<AbsLinuxFile?>(
                        """
                    val f =
                        openFile {
                            extensionFilter("model files", FileExtension.MODEL)
                        }?.toAbsLinuxFile()
                        """.trimIndent()
                    )
                if (f != null) {
                    state.tests.value = null
                    state.model.value = f
                }
            }

            Column(
                modifier = Modifier.height(prefButtonHeight.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DeephyButton(s = "Give me a demo!", modifier = Modifier.height(prefButtonHeight.value)) {
                    app.showDemos()
                }.apply {

                            /*font = font.fixed().copy("Arial", size = 18.0, weight = BOLD).fx()



                    this.fill = FXColor.ORANGE



                    cursor = Cursor.HAND*/
                }
            }

            /*tooltip has to be outside of checkbox or else it will not show when checkbox is disabled?*/
            DeephysTooltipArea(s = visualizerToolTipText.value, settings = settings) {
                Row {

                    disabledCode {
                        /*- Model Diagram is removed (for now). Maybe this will be added later. The cost of maintaining this feature is currently not worth its value.*/
                        DeephyCheckbox(
                            s = "Show Model Diagram",
                            modifier = Modifier.height(prefButtonHeight.value),
                            prop = showVisualizer
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.End) {
                DeephyButton(
                    Modifier.observeDpHeight(prefButtonHeight)
                ) {

                    unsafeError(
                        """
                                      graphic =
                                    ImageViewWrapper(gearImage.await()).apply {
                                        isPreserveRatio = true
                                        fitWidth = 25.0
                                    }
                        """.trimIndent()
                    )
                    showSettingsWindow()
                }
            }
        }

        if (showVisualizer.value) {
            Column {
                LoadSwapper(
                    loadedModel.value,
                    nullMessage = "Select a .model file to begin"
                ) {
                    val model = this
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DeephysText(s = "Model: ${model.name}", style = titleFont())
                            Row(horizontalArrangement = Arrangement.spacedBy(DEEPHYS_SYMBOL_SPACING.dp)) {
                                /*wrapTextProp v true*/
                                DeephysInfoSymbol(
                                    string {
                                        lineDelimited {
                                            +"Layers"
                                            model.layers.forEach {
                                                +"\t${it.layerID.truncateWithEllipsesOrAddSpaces(15)}: ${it.neurons.size}"
                                            }
                                        }
                                    },
                                    font = StandardFonts.Monospaced
                                )

                                /*
                                        deephysTutorialSymbol(
                                          "This is a model, which is a definition of how many layers and neurons you have used."
                                        )*/

                                if (model.wasLoadedWithSuffix) {
                                    DeephysWarningSymbol(SUFFIX_WARNING)
                                }
                            }
                        }

                        println("loaded model: " + model.infoString())

                        val maxNeurons = 50

                        val vis =
                            if (model.layers.all { it.neurons.size <= maxNeurons }) {
                                visualizerToolTipText v "Show an interactive diagram of the model"
                                ModelVisualizer(
                                    state = modelVisualizerState,
                                    model = model,
                                    settings = settings
                                )
                            } else {
                                visualizerToolTipText v "model is too large to visualize (>$maxNeurons in a layer)"
                                null
                            }
                        modelVisualizerState.dsetViewsBox = dsetViews
                        val theTests = state.tests.value
                        theTests?.go {
                            dsetViews += it
                        }

                        DSetViewsVBox(
                            dsetViews,
                            model,
                            settings
                        )

                        DeephysTooltipArea(
                            settings,
                            "Add a test"
                        ) {
                            DeephyIconButton("icon/plus") {
                                val _ = dsetViews.addTest()
                            }
                        }
                        runBlocking {
                            app.testReadyDSetViewsBbox.emit(dsetViews)
                        }
                    }
                }
            }
        }
    }
}
