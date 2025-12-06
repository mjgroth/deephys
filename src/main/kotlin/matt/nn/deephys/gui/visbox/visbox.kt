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
import matt.compose.graphics.defaults.StandardFonts
import matt.lang.common.disabledCode
import matt.lang.common.unsafeError
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.global.DeephyButton
import matt.nn.deephys.gui.global.DeephyIconButton
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.global.tooltip.SUFFIX_WARNING
import matt.nn.deephys.gui.global.tooltip.symbol.DEEPHYS_SYMBOL_SPACING
import matt.nn.deephys.gui.global.tooltip.symbol.DeephysInfoSymbol
import matt.nn.deephys.gui.global.tooltip.symbol.DeephysWarningSymbol
import matt.nn.deephys.gui.modelvis.ModelVisualizerState
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.load.CborSyncLoadResult
import matt.nn.deephys.load.LoadSwapper
import matt.nn.deephys.model.importformat.Model
import matt.obs.prop.writable.BindableProperty
import matt.obs.prop.writable.v
import matt.prim.common.exportfromlang.model.file.FsFile
import matt.prim.str.mybuild.api.string
import matt.prim.str.truncateWithEllipsesOrAddSpaces

@Suppress("UnusedParameter", "unused")
@Composable
fun VisBox(
    app: DeephysApp,
    settings: DeephysSettingsController,
    loadedModel: State<CborSyncLoadResult<Model>?>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        fun load(
            modelFile: FsFile,
            testFiles: List<FsFile>
        ) {
            unsafeError(
                """
            DeephyState.model.value = modelFile.toAbsLinuxFile()
            val dSetViewsVBox = findRecursivelyFirstOrNull<DSetViewsVBox>() ?: error("no dset views box!")
            dSetViewsVBox.removeAllTests()
            /*findRecursivelyFirstOrNull<DSetViewsVBox>()?.removeAllTests()*/
            testFiles.forEach { f ->
                val viewer = dSetViewsVBox.addTest()
                viewer.file.value = (mFile(f.path, MacFileSystem)).checkType(Cbor)
            }      
                """.trimIndent()
            )
        }

        /*val settingsButton = settButton

  val prefButtonHeight = settingsButton.heightProperty*/

        val prefButtonHeight = BindableProperty(25.0)
        val visualizer = BindableProperty<ModelVisualizerState?>(null)
        val pleaseLoadModelToSeeVisualizerText = "Choose a model in order to visualize it"
        val visualizerToolTipText = BindableProperty(pleaseLoadModelToSeeVisualizerText)
        val showVisualizer = BindableProperty(false)

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            DeephyButton(s = "Select Model") {
                unsafeError(
                    """
                prefHeightProperty.bind(prefButtonHeight)
                setOnAction {
                    val f =
                        openFile {
                            extensionFilter("model files", FileExtension.MODEL)
                        }?.toAbsLinuxFile()

                    if (f != null) {
                        DeephyState.tests.value = null
                        DeephyState.model.value = f
                    }
                }     
                    """.trimIndent()
                )
            }

            Column(
                modifier = Modifier.height(prefButtonHeight.value.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DeephyButton(s = "Give me a demo!", modifier = Modifier.height(prefButtonHeight.value.dp)) {
                    unsafeError(
                        """
                    this@VisBox.app.showDemos()        
                        """.trimIndent()
                    )
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
                        unsafeError(
                            """
                        
                        /*- Model Diagram is removed (for now). Maybe this will be added later. The cost of maintaining this feature is currently not worth its value.*/
                        DeephyCheckbox("Show Model Diagram", modifier = Modifier.height(prefButtonHeight.value.dp), enabled = visualizer.value != null) {
                            visualizer.onChange {
                                if (it == null) {
                                    isSelected = false
                                }
                            }
                            showVisualizer.bind(selectedProperty)
                        }          
                            """.trimIndent()
                        )
                    }
                }
            }

                    /*	  h {
                        hgrow = ALWAYS
                        alignment = Pos.CENTER_RIGHT
                        +settingsButton
                      }*/
        }

        if (showVisualizer.value) {
            Column {
                unsafeError(
                    """
                           swap(visualizer)        
                    """.trimIndent()
                )
            }
        }

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
                        unsafeError(
                            """
                        ModelVisualizer(model, settings)    
                            """.trimIndent()
                        )
                    } else {
                        visualizerToolTipText v "model is too large to visualize (>$maxNeurons in a layer)"
                        null
                    }
                unsafeError(
                    """
                     visualizer v vis

                val dSetViewsBox = DSetViewsVBox(model, settings)
                dSetViewsBox.modelVisualizer = vis
                vis?.dsetViewsBox = dSetViewsBox
                val theTests = DeephyState.tests.value
                theTests?.go {
                    dSetViewsBox += it
                } 
                +dSetViewsBox
                    """.trimIndent()
                )

                DeephysTooltipArea(
                    settings,
                    "Add a test"
                ) {
                    DeephyIconButton("icon/plus") {
                        unsafeError("""dSetViewsBox.addTest()""")
                    }
                }
                unsafeError(
                    """
    app.testReadyDSetViewsBbox.page(dSetViewsBox)
                    """.trimIndent()
                )
            }
        }
    }
}
