package matt.nn.deephys.gui.settings.gui

import javafx.scene.control.TreeItem
import javafx.scene.text.TextAlignment.CENTER
import javafx.stage.Modality.APPLICATION_MODAL
import javafx.stage.StageStyle
import matt.fx.control.inter.graphic
import matt.fx.control.wrapper.control.tree.treeview
import matt.fx.control.wrapper.treeitem.TreeItemWrapper
import matt.fx.graphics.wrapper.imageview.ImageViewWrapper
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.stack.stackpane
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.gui.interact.WindowConfig
import matt.gui.mstage.MStage
import matt.gui.mstage.ShowMode.DO_NOT_SHOW
import matt.gui.mstage.WMode.CLOSE
import matt.gui.option.EnumSetting
import matt.gui.option.SettingsData
import matt.lang.assertions.require.requireNull
import matt.lang.common.go
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyRadioButton
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.settings.gui.control.createControlFor
import matt.nn.deephys.init.gearImage


class SettingsWindow(settings: DeephysSettingsController) : MStage() {
    companion object {
        private var instance: SettingsWindow? = null
    }

    init {
        synchronized(SettingsWindow::class) {
            requireNull(instance)
            instance = this
        }
    }

    fun setupFor(settings: DeephysSettingsController) {
        WindowConfig(
            showMode = DO_NOT_SHOW,
            modality = APPLICATION_MODAL,
            wMode = CLOSE,
            EscClosable = true,
            decorated = true,
            title = "Deephys Options"
        ).applyTo(this, SettingsPane(settings))
        width = 1000.0
    }

    init {
        setupFor(settings)
    }

    fun button(receiver: NodeWrapper) =
        receiver.deephyButton {

            graphic =
                ImageViewWrapper(gearImage.await()).apply {
                    isPreserveRatio = true
                    fitWidth = 25.0
                }
            setOnAction {
                this@SettingsWindow.initStyle(StageStyle.DECORATED)
                if (!this@SettingsWindow.isShowing) {
                    if (this@SettingsWindow.owner == null) {
                        receiver.stage?.go {
                            this@SettingsWindow.initOwner(it)
                        }
                    }
                    println("waiting...")
                    this@SettingsWindow.showAndWait()
                    println("done waiting")
                }
            }
        }
}


fun <E : Enum<E>> EnumSetting<E>.createRadioButtons(rec: NodeWrapper) =
    rec.apply {
        val tm = createBoundToggleMechanism()
        cls.java.enumConstants.forEach {
            deephyRadioButton((it as Enum<*>).name, tm, it) {
                isSelected = prop.value == it
            }
        }
    }

class SettingsPane(override val settings: DeephysSettingsController) : VBoxWrapperImpl<NodeWrapper>(), DeephysNode {
    companion object {
        private var instance: SettingsPane? = null
    }

    init {
        synchronized(SettingsPane::class) {
            requireNull(instance)
            instance = this
        }
    }

    init {

        val memSafeSettings = settings

        h {
            val tv =
                treeview<SettingsData> {
                    root = TreeItemWrapper(memSafeSettings)
                    populate {
                        it.value.sections.map { it as SettingsData }
                    }
                    root!!.expandAll()
                    select(root!!.node)
                }
            v {
                fun update(selection: TreeItem<SettingsData>?) {
                    clear()
                    selection?.value?.settings?.forEach { sett ->
                        +createControlFor(sett, memSafeSettings)
                    } ?: run {
                        stackpane<NW> {
                            prefHeightProperty.bindWeakly(this@v.heightProperty)
                            prefWidthProperty.bindWeakly(this@v.widthProperty)
                            deephysText("Select a section in the tree to edit its settings.") {
                                textAlignment = CENTER
                            }
                        }
                    }
                }
                tv.selectedItemProperty.onChange {
                    update(it)
                }
                update(tv.selectedItem)
            }
        }
    }
}
