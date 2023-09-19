package matt.nn.deephys.gui.visbox

import javafx.application.Platform
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.TOP_CENTER
import matt.file.construct.mFile
import matt.file.ext.FileExtension
import matt.file.toMacFile
import matt.file.types.checkType
import matt.fx.graphics.dialog.openFile
import matt.fx.graphics.fxthread.runLater
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.node.disableWhen
import matt.fx.graphics.wrapper.node.findRecursivelyFirstOrNull
import matt.fx.graphics.wrapper.node.visibleAndManagedWhen
import matt.fx.graphics.wrapper.pane.anchor.swapper.swap
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.lang.disabledCode
import matt.lang.go
import matt.lang.model.file.FsFile
import matt.lang.model.file.MacFileSystem
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.DEEPHYS_FONT_MONO
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyCheckbox
import matt.nn.deephys.gui.global.deephyIconButton
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.SUFFIX_WARNING
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.global.tooltip.symbol.DEEPHYS_SYMBOL_SPACING
import matt.nn.deephys.gui.global.tooltip.symbol.deephysInfoSymbol
import matt.nn.deephys.gui.global.tooltip.symbol.deephysWarningSymbol
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.init.modelBinding
import matt.nn.deephys.load.loadSwapper
import matt.nn.deephys.state.DeephyState
import matt.obs.prop.BindableProperty
import matt.prim.str.mybuild.api.string
import matt.prim.str.truncateWithElipsesOrAddSpaces


class VisBox(
    private val app: DeephysApp,
    settings: DeephysSettingsController,
) : VBoxW() {


    fun load(
        modelFile: FsFile,
        testFiles: List<FsFile>
    ) {
        DeephyState.model.value = modelFile.toMacFile()
        runLater {
            val dSetViewsVBox = findRecursivelyFirstOrNull<DSetViewsVBox>() ?: error("no dset views box!")
            dSetViewsVBox.removeAllTests()
            runLater {
                /*findRecursivelyFirstOrNull<DSetViewsVBox>()?.removeAllTests()*/
                testFiles.forEach { f ->
                    val viewer = dSetViewsVBox.addTest()
                    viewer.file.value = (mFile(f.path, MacFileSystem)).checkType()
                }
            }
        }

    }


    init {

        /*val settingsButton = settButton*/

        /*val prefButtonHeight = settingsButton.heightProperty*/
        val prefButtonHeight = BindableProperty(25.0)

        spacing = 10.0
        alignment = TOP_CENTER

        val visualizer = BindableProperty<ModelVisualizer?>(null)
        val pleaseLoadModelToSeeVisualizerText = "Choose a model in order to visualize it"
        val visualizerToolTipText = BindableProperty(pleaseLoadModelToSeeVisualizerText)
        val showVisualizer = BindableProperty(false)

        h {
            spacing = 5.0

            deephyButton("Select Model") {
                prefHeightProperty.bind(prefButtonHeight)
                setOnAction {


                    val f = openFile {
                        extensionFilter("model files", FileExtension.MODEL)
                    }?.toMacFile()

                    if (f != null) {
                        DeephyState.tests.value = null
                        DeephyState.model.value = f
                    }
                }
            }


            v {
                prefHeightProperty.bind(prefButtonHeight)
                alignment = CENTER
                deephyButton("Give me a demo!") {
                    prefHeightProperty.bind(prefButtonHeight)
                    setOnAction {
                        this@VisBox.app.showDemos()
                    }

                }.apply {

                    /*font = font.fixed().copy("Arial", size = 18.0, weight = BOLD).fx()*/

                    /*this.fill = FXColor.ORANGE*/
                    /*cursor = Cursor.HAND*/

                }
            }


            h {

                deephyTooltip(
                    visualizerToolTipText,
                    settings = settings
                ) /*tooltip has to be outside of checkbox or else it will not show when checkbox is disabled?*/



                disabledCode {
                    /*- Model Diagram is removed (for now). Maybe this will be added later. The cost of maintaining this feature is currently not worth its value.*/
                    deephyCheckbox("Show Model Diagram") {
                        prefHeightProperty.bind(prefButtonHeight)
                        visualizer.onChange {
                            if (it == null) {
                                isSelected = false
                            }
                        }
                        disableWhen { visualizer.isNull }

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



        v {
            visibleAndManagedWhen { showVisualizer }
            swap(visualizer)
        }


        loadSwapper(modelBinding.await(), nullMessage = "Select a .model file to begin") {
            val model = this@loadSwapper
            VBoxWrapperImpl<NodeWrapper>().apply {

                h {
                    spacing = 10.0
                    deephysText("Model: ${model.name}") {
                        titleFont()
                    }

                    h {
                        spacing = DEEPHYS_SYMBOL_SPACING



                        deephysInfoSymbol(
                            string {
                                lineDelimited {
                                    +"Layers"
                                    model.layers.forEach {
                                        +"\t${it.layerID.truncateWithElipsesOrAddSpaces(15)}: ${it.neurons.size}"
                                    }
                                }
                            }
                        ) {
                            fontProperty v DEEPHYS_FONT_MONO
                            /*wrapTextProp v true*/
                        }

                        /*
                                    deephysTutorialSymbol(
                                      "This is a model, which is a definition of how many layers and neurons you have used."
                                    )*/

                        if (model.wasLoadedWithSuffix) {
                            deephysWarningSymbol(SUFFIX_WARNING)
                        }
                    }

                }


                println("loaded model: " + model.infoString())

                val maxNeurons = 50


                val vis = if (model.layers.all { it.neurons.size <= maxNeurons }) {
                    visualizerToolTipText v "Show an interactive diagram of the model"
                    ModelVisualizer(model, settings)
                } else {
                    visualizerToolTipText v "model is too large to visualize (>$maxNeurons in a layer)"
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
                deephyIconButton("icon/plus") {
                    deephyTooltip("Add a test", settings = settings)
                    setOnAction {
                        dSetViewsBox.addTest()
                    }

                }
                Platform.runLater {
                    this@VisBox.app.testReadyDSetViewsBbox.page(dSetViewsBox)
                }
            }
        }

    }
}