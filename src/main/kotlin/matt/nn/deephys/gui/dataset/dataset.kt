package matt.nn.deephys.gui.dataset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import matt.compose.controls.choicebox.MyChoiceBox
import matt.compose.controls.tabpane.TabPane
import matt.compose.graphics.text.MyText
import matt.lang.common.DoNothing
import matt.lang.common.unsafeError
import matt.lang.common.unsafeReturningErr
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dataset.bycategory.ByCategoryView
import matt.nn.deephys.gui.dataset.byimage.ByImageView
import matt.nn.deephys.gui.dataset.byneuron.ByNeuronView
import matt.nn.deephys.gui.global.DeephysLabeledControl2
import matt.nn.deephys.gui.global.tooltip.symbol.DEEPHYS_SYMBOL_SPACING
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.load.test.TestLoader

enum class DatasetNodeView { ByNeuron, ByImage, ByCategory }

@Composable
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
                DeephysLabeledControl2(
                    "Layer"
                ) {
                    MyChoiceBox(
                        viewer.layerSelection.value,
                        choices = unsafeReturningErr("""viewer.model.resolvedLayers.map { it.interTest },"""),
                        onChoose = {
                            viewer.layerSelection.value = it
                        }
                    )
                }
            }
            unsafeError("this should have the style of a DeephysTabPane")
            TabPane(
                onSelected = {
                    viewer.navigateTo(
                        it
                    )
                }
            ) {
                DatasetNodeView.entries.forEach {
                    Tab(
                        id = it,
                        selected = viewer.view.value == it,
                        tab = {
                            MyText(it.name)
                        }
                    ) {
                        DoNothing
                    }
                }
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
