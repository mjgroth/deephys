package matt.nn.deephys.gui.settings.gui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import matt.exec.option.EnumSetting
import matt.lang.common.unsafeErr
import matt.nn.deephys.gui.settings.DeephysSettingsController

@Composable
fun SettingsWindow(settings: DeephysSettingsController) {
    unsafeErr(
        """
                   MyWindow {

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
        """.trimIndent()
    )
}

@Suppress("UnusedReceiverParameter")
@Composable
fun <E : Enum<E>> EnumSetting<E>.createRadioButtons() {
    unsafeErr(
        """
        val tm = createBoundToggleMechanism()
        cls.java.enumConstants.forEach {
            DeephyRadioButton((it as Enum<*>).name, tm, it) {
                isSelected = prop.value == it
            }
        }      
        """.trimIndent()
    )
}

@Composable
fun SettingsPane(settings: DeephysSettingsController) =
    Column {

        val memSafeSettings = settings

        Row {
            unsafeErr(
                """
                val tv =
                    treeview<SettingsData> {
                        root = TreeItemWrapper(memSafeSettings)
                        populate {
                            it.value.sections.map { it as SettingsData }
                        }
                        root!!.expandAll()
                        select(root!!.node)
                    }
                Column {
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
                """.trimIndent()
            )
        }
    }
