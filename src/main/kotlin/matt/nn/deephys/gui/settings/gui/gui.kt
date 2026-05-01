@file:Suppress("UnusedParameter")

package matt.nn.deephys.gui.settings.gui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import matt.compose.controls.tree.MyTree
import matt.compose.state.option.SettingsData
import matt.lang.err.unsafeError
import matt.lang.err.unsafeReturningErr
import matt.lang.todo.TODO_NO_DETAILS
import matt.nn.deephys.gui.settings.DeephysSettingsController

@Suppress("unused")
@Composable
fun SettingsWindow(
    settings: DeephysSettingsController
) {

    unsafeError(
        "this window was MODAL or whatever (it completely blocked the GUI thread in the underlying app while it was open..."
    )

    Window(
        onCloseRequest = {
            TODO_NO_DETAILS()
        },
        state = rememberWindowState(width = 1000.dp)

    ) {

        unsafeError(
            """
            fun setupFor(settings: DeephysSettingsController) {
                WindowConfig(
                    showMode = DO_NOT_SHOW,
                    modality = APPLICATION_MODAL,
                    wMode = CLOSE,
                    EscClosable = true,
                    decorated = true,
                    title = "Deephys Options"
                ).applyTo(this, SettingsPane(settings))
            }

            init {
                setupFor(settings)
            }
            """.trimIndent()
        )
    }
}

@Suppress("unused")
@Composable
fun SettingsPane(settings: DeephysSettingsController) =
    Column {

        Row {

            MyTree<SettingsData>(
                root = settings,
                populate = {
                    unsafeReturningErr("it.value.sections.map { it as SettingsData }")
                }
            ) {
                unsafeError(
                    """
                           root!!.expandAll()
                        select(root!!.node)
                    """.trimIndent()
                )
            }

            Column {
                fun update(selection: SettingsData?) {
                    unsafeError(
                        """
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
                        """.trimIndent()
                    )
                }

                unsafeError(
                    """
                    tv.selectedItemProperty.onChange {
                        update(it)
                    }
                    """.trimIndent()
                )
                update(unsafeReturningErr("tv.selectedItem"))
            }
        }
    }
