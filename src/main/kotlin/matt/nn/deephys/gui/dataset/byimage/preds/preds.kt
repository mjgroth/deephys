package matt.nn.deephys.gui.dataset.byimage.preds

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import matt.caching.compcache.invoke
import matt.compose.controls.toggleicon.ToggleIcon
import matt.compose.state.lang.not
import matt.compose.state.rememberMutableStateOf
import matt.lang.common.unsafeErr
import matt.lang.weak.common.WeakRefInter
import matt.nn.deephys.calc.ImageTopPredictions
import matt.nn.deephys.gui.global.DeephyActionLabel
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.SigFigText
import matt.nn.deephys.gui.global.SpacerWithOldFxSize
import matt.nn.deephys.gui.global.color.DeephysPalette
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.model.data.Category

@Composable
fun PredictionsView(
    groundTruth: Category,
    topPreds: ImageTopPredictions<*>,
    weakViewer: WeakRefInter<DatasetViewerState>,
    settings: DeephysSettingsController
) {
    Column {
        Row {
            DeephysText("Ground Truth: ", font = titleFont())
            DeephyActionLabel(groundTruth.label, font = titleBoldFont()) {
                weakViewer.deref()!!.navigateTo(groundTruth)
            }
        }
        SpacerWithOldFxSize()
        DeephysText("Predictions: ", font = titleFont())
        with(weakViewer.deref()!!.testData.value!!.testRAMCache) {
            CategoryTable(
                title = "",
                titleUnfolded = "",
                data = topPreds().map { it.first to it.second },
                settings = settings,
                weakViewer = weakViewer,
                sigFigSett = weakViewer.deref()!!.predictionSigFigs,
                tooltip = "Top classification layer output values. Numbers displayed have been run through a softmax."
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryTable(
    title: String,
    titleUnfolded: String,
    data: List<Pair<Category, Number>>,
    weakViewer: WeakRefInter<DatasetViewerState>,
    settings: DeephysSettingsController,
    tooltip: String,
    sigFigSett: State<Int>,
    numSuffix: String = ""
) = Row {
    val b = rememberMutableStateOf(false)
    ToggleIcon(
        tooltip = "idk",
        falseIcon = Icons.Default.Add,
        trueIcon = Icons.Default.Remove,
        state = b,
        tint = DeephysPalette.deephysBlue2,
        modifier = Modifier.size((6.5 / 2).dp)
    )
    Spacer(Modifier.width(5.dp))
    Column {
        if (b.not().value) {
            FlowRow {
                DeephysText(titleUnfolded)
                data.forEach { (cat, num) ->
                    val fullString = "${cat.label} ($num)"
                    cat.actionText(
                        tooltip = fullString,
                        settings = settings,
                        weakViewer = weakViewer,
                        allowedLengths = 1..10
                    )
                    DeephysText(" (")
                    SigFigText(
                        num = num,
                        sigFigSett = sigFigSett,
                        numSuffix = numSuffix,
                        settings =  settings,
                        tooltip = fullString
                    )
                    DeephysText(")   ")
                }
            }
        }
        if (b.value) {
            Column {
                DeephysTooltipArea(settings = settings, tooltip) {
                    DeephysText(title, font = subtitleFont())
                }

                Spacer(Modifier.size(1.dp))
                Column {

                    Spacer(Modifier.size(2.dp))

                    Row {
                        val predNamesBox = Column {}
                        SpacerWithOldFxSize()
                        val predValuesBox = Column {}
                    }
                    data.forEach {
                        val category = it.first
                        val num = it.second

                        val fullString = "${category.label} ($num)"
                        unsafeErr(
                            """
                            category.actionText(
                                r = predNamesBox,
                                tooltip = fullString,
                                settings = memSafeSettings,
                                weakViewer = weakViewer
                            )

                            predValuesBox
                                .sigFigText(
                                    num = num,
                                    sigFigSett = this@CategoryTable.sigFigSett,
                                    numSuffix = numSuffix,
                                    settings = memSafeSettings,
                                    tooltip = fullString
                                )           
                            """.trimIndent()
                        )
                    }
                }
            }
        }
    }
}
