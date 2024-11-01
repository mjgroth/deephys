package matt.nn.deephys.gui.visbox


import androidx.compose.runtime.Composable
import matt.lang.common.unsafeErr
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.settings.DeephysSettingsController


@Composable
fun VisBox(
    app: DeephysApp,
    settings: DeephysSettingsController
) {
    unsafeErr(
        """
        Column {


            fun load(
                modelFile: FsFile,
                testFiles: List<FsFile>
            ) {
                DeephyState.model.value = modelFile.toAbsLinuxFile()
                val dSetViewsVBox = findRecursivelyFirstOrNull<DSetViewsVBox>() ?: error("no dset views box!")
                dSetViewsVBox.removeAllTests()
                /*findRecursivelyFirstOrNull<DSetViewsVBox>()?.removeAllTests()*/
                testFiles.forEach { f ->
                    val viewer = dSetViewsVBox.addTest()
                    viewer.file.value = (mFile(f.path, MacFileSystem)).checkType(Cbor)
                }
            }


            init {

                /*val settingsButton = settButton

            val prefButtonHeight = settingsButton.heightProperty*/
                val prefButtonHeight = BindableProperty(25.0)

                spacing = 10.0
                alignment = TOP_CENTER

                val visualizer = BindableProperty<ModelVisualizerState?>(null)
                val pleaseLoadModelToSeeVisualizerText = "Choose a model in order to visualize it"
                val visualizerToolTipText = BindableProperty(pleaseLoadModelToSeeVisualizerText)
                val showVisualizer = BindableProperty(false)

                Row {
                    spacing = 5.0

                    DeephysButton("Select Model") {
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
                    }


                    Column(
                        modifier = Modifier.height(prefButtonHeight.value.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DeephyButton("Give me a demo!", modifier = Modifier.height(prefButtonHeight.value.dp)) {
                            this@VisBox.app.showDemos()
                        }.apply {

                            /*font = font.fixed().copy("Arial", size = 18.0, weight = BOLD).fx()



                    this.fill = FXColor.ORANGE



                    cursor = Cursor.HAND*/
                        }
                    }


                    Row {

                        deephyTooltip(
                            visualizerToolTipText,
                            settings = settings
                        ) /*tooltip has to be outside of checkbox or else it will not show when checkbox is disabled?*/



                        disabledCode {
                            /*- Model Diagram is removed (for now). Maybe this will be added later. The cost of maintaining this feature is currently not worth its value.*/
                            DeephyCheckbox("Show Model Diagram", modifier = Modifier.height(prefButtonHeight.value.dp), enabled = visualizer.value != null) {
                                visualizer.onChange {
                                    if (it == null) {
                                        isSelected = false
                                    }
                                }
                                showVisualizer.bind(selectedProperty)
                            }
                        }
                    }

                    /*	  h {
                        hgrow = ALWAYS
                        alignment = Pos.CENTER_RIGHT
                        +settingsButton
                      }*/
                }



                Column {
                    visibleAndManagedWhen { showVisualizer }
                    swap(visualizer)
                }


                loadSwapper(modelBinding.await(), nullMessage = "Select a .model file to begin") {
                    val model = this@loadSwapper
                    Column {

                        Row {
                            spacing = 10.0
                            deephysText("Model: ${'$'}{model.name}") {
                                titleFont()
                            }

                            Row {
                                spacing = DEEPHYS_SYMBOL_SPACING



                                DeephysInfoSymbol(
                                    string {
                                        lineDelimited {
                                            +"Layers"
                                            model.layers.forEach {
                                                +"\t${'$'}{it.layerID.truncateWithElipsesOrAddSpaces(15)}: ${'$'}{it.neurons.size}"
                                            }
                                        }
                                    }
                                ) {
                                    fontProperty v MONO_FONT
                                    /*wrapTextProp v true*/
                                }

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
                                ModelVisualizer(model, settings)
                            } else {
                                visualizerToolTipText v "model is too large to visualize (>${'$'}maxNeurons in a layer)"
                                null
                            }
                        visualizer v vis

                        val dSetViewsBox = DSetViewsVBox(model, settings)
                        dSetViewsBox.modelVisualizer = vis
                        vis?.dsetViewsBox = dSetViewsBox
                        val theTests = DeephyState.tests.value
                        theTests?.go {
                            dSetViewsBox += it
                        }
                        +dSetViewsBox
                        DeephysTooltipArea(
                            settings,
                            "Add a test"
                        ) {
                            DeephyIconButton("icon/plus") {
                                dSetViewsBox.addTest()
                            }
                        }

                        this@VisBox.app.testReadyDSetViewsBbox.page(dSetViewsBox)
                    }
                }
            }
        }
        """.trimIndent()
    )
}
