package matt.nn.deephys.gui.dataset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import matt.compose.controls.choicebox.MyChoiceBox
import matt.compose.controls.tabpane.state.SimpleTabPaneController
import matt.lang.collect.indexOfSingle
import matt.lang.err.unsafeReturningErr
import matt.lang.nop.DoNothing
import matt.lang.safeconvert.verifyToInt
import matt.lang.safeconvert.verifyToUInt
import matt.model.k.log.Logger
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dataset.bycategory.ByCategoryView
import matt.nn.deephys.gui.dataset.byimage.ByImageView
import matt.nn.deephys.gui.dataset.byneuron.ByNeuronView
import matt.nn.deephys.gui.dataset.dtab.DeephysTabPane
import matt.nn.deephys.gui.global.DeephysLabeledControl
import matt.nn.deephys.gui.global.tooltip.symbol.DEEPHYS_SYMBOL_SPACING
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.load.test.TestLoader

enum class DatasetNodeView { ByNeuron, ByImage, ByCategory }

@Composable
context(_: Logger)
fun DatasetNode(
    dataset: TestLoader,
    viewer: DatasetViewerState,
    settings: DeephysSettingsController
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DEEPHYS_SYMBOL_SPACING.dp)
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy((DEEPHYS_SYMBOL_SPACING * 2).dp)
        ) {
            if (viewer.isUnboundToDSet.value) {
                DeephysLabeledControl(
                    "Layer",
                    spacerWidth = 5.dp,
                    controlWidth = 500.dp
                ) {
                    MyChoiceBox(
                        viewer.layerSelection.value,
                        choices = unsafeReturningErr("""viewer.model.resolvedLayers.map { it.interTest },"""),
                        onChoose = {
                            viewer.manualLayerSelected.value = it
                        }
                    )
                }
            }

            val selectedIndex =
                DatasetNodeView
                    .entries
                    .indexOfSingle(viewer.view.value)
                    .verifyToUInt()
            /*invalidate tab pane state with selection index as hack to retain behavior*/
            val controller =
                remember(selectedIndex) {
                    SimpleTabPaneController(selectedIndex)
                }
            DeephysTabPane(controller = controller) {
                DatasetNodeView.entries.forEach {
                    Tab(it.name) {
                        DoNothing
                    }
                }
            }

            val actualSelectedIndex = controller.selectedIndex.value
            LaunchedEffect(actualSelectedIndex) {
                /*hack to retain behavior but fit modern code*/
                viewer.navigateTo(
                    DatasetNodeView.entries[actualSelectedIndex.verifyToInt()]
                )
            }

            when (viewer.view.value) {
                ByNeuron   -> {
                    ByNeuronView(
                        dataset,
                        viewer,
                        settings = settings
                    )
                }

                ByImage    -> {
                    ByImageView(
                        dataset.postDtypeTestLoader.awaitRequireSuccessful().preppedTest.awaitRequireSuccessful(),
                        viewer,
                        settings = settings
                    )
                }

                ByCategory -> {
                    ByCategoryView(
                        dataset.postDtypeTestLoader.awaitRequireSuccessful().preppedTest.awaitRequireSuccessful(),
                        viewer,
                        settings = settings,
                        viewerWidth = unsafeReturningErr()
                    )
                }
            }
        }
    }
}
