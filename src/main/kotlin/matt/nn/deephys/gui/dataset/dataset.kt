package matt.nn.deephys.gui.dataset

import javafx.scene.input.MouseEvent
import matt.fx.control.wrapper.control.choice.choicebox
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.lang.weak.common.WeakRefInter
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dataset.bycategory.ByCategoryView
import matt.nn.deephys.gui.dataset.byimage.ByImageView
import matt.nn.deephys.gui.dataset.byneuron.ByNeuronView
import matt.nn.deephys.gui.dataset.dtab.DeephysTabPane
import matt.nn.deephys.gui.global.configForDeephys
import matt.nn.deephys.gui.global.deephysLabeledControl2
import matt.nn.deephys.gui.global.tooltip.symbol.DEEPHYS_SYMBOL_SPACING
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import matt.obs.bind.binding
import matt.obs.prop.ObsVal
import matt.obs.prop.writable.BindableProperty
import java.lang.ref.WeakReference

enum class DatasetNodeView {
    ByNeuron, ByImage, ByCategory
}

class DatasetNode(
    dataset: TestLoader,
    viewer: DatasetViewer,
    override val settings: DeephysSettingsController
): VBoxW(childClass = NodeWrapper::class), DeephysNode {

    val weakViewer = WeakReference(viewer)

    private val weakSettings = WeakReference(settings)

    private val byNeuronView by lazy {
        ByNeuronView(
            dataset,
            viewer,
            settings = weakSettings.get()!!
        )
    }
    private val byImageView by lazy {
        ByImageView(
            dataset.postDtypeTestLoader.awaitRequireSuccessful().preppedTest.awaitRequireSuccessful(),
            viewer,
            settings = weakSettings.get()!!
        )
    }
    private val byCategoryView by lazy {
        ByCategoryView(
            dataset.postDtypeTestLoader.awaitRequireSuccessful().preppedTest.awaitRequireSuccessful(),
            viewer,
            settings = weakSettings.get()!!
        )
    }

    init {

        spacing = DEEPHYS_SYMBOL_SPACING
        isFillWidth = false


        val layerCB =
            choicebox(
                nullableProp = viewer.layerSelection,
                values = viewer.model.resolvedLayers.map { it.interTest }
            ) {
                configForDeephys()
                valueProperty.onChange {
                    println("layerCB value changed to $it")
                }
            }

        val layerController =
            deephysLabeledControl2(
                "Layer",
                layerCB
            ) {
                visibleAndManagedProp.bind(viewer.isUnboundToDSet)
            }

        val topHBox =
            h {
                spacing = DEEPHYS_SYMBOL_SPACING * 2
                +layerController
            }
        val mainControl = BindableProperty<RegionWrapper<*>?>(null)
        val mainControlSwapper =
            swapper(mainControl) {
                this
            }

        val tabPane =
            DeephysTabPane().apply {
                layerController.prefWidthProperty.bind(tabBar.widthProperty)
                val neuronTab =
                    deephysLazyTab("Neuron") {
                        this@DatasetNode.byNeuronView
                    }.apply {
                        addEventFilter(MouseEvent.MOUSE_PRESSED) {
                            it.consume()
                            if (!isSelected) {
                                this@DatasetNode.weakViewer.get()!!.navigateTo(
                                    ByNeuron
                                )
                            }
                        }
                    }
                val imageTab =
                    deephysLazyTab("Image") {
                        this@DatasetNode.byImageView
                    }.apply {
                        addEventFilter(MouseEvent.MOUSE_PRESSED) {
                            it.consume()
                            if (!isSelected) {
                                this@DatasetNode.weakViewer.get()!!.navigateTo(
                                    ByImage
                                )
                            }
                        }
                    }
                val categoryTab =
                    deephysLazyTab("Category") {
                        this@DatasetNode.byCategoryView
                    }.apply {
                        addEventFilter(MouseEvent.MOUSE_PRESSED) {
                            it.consume()
                            if (!isSelected) {
                                this@DatasetNode.weakViewer.get()!!.navigateTo(
                                    ByCategory
                                )
                            }
                        }
                    }


                fun update(view: DatasetNodeView) {
                    when (view) {
                        ByNeuron -> {
                            neuronTab.fire()
                            mainControl.unbind()
                            mainControl.bindWeakly(this@DatasetNode.byNeuronView.control.binding { it?.deref() })
                        }

                        ByImage -> {
                            imageTab.fire()
                            mainControl.unbind()
                            mainControl.bindWeakly(this@DatasetNode.byImageView.control.binding { it?.deref() })
                        }

                        ByCategory -> {
                            categoryTab.fire()
                            mainControl.unbind()
                            mainControl.bindWeakly(this@DatasetNode.byCategoryView.control.binding { it?.deref() })
                        }
                    }
                }
                update(viewer.view.value)
                viewer.view.onChange {
                    update(it)
                }
            }
        +tabPane
        topHBox += tabPane.tabBar
        topHBox += mainControlSwapper
    }
}

interface MainDeephysView: DeephysNode {
    val control: ObsVal<WeakRefInter<RegionWrapper<*>>?>
}
