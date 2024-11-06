package matt.nn.deephys.gui.category

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import matt.caching.compcache.invoke
import matt.color.colormap.Automatic
import matt.color.common.FloatColor
import matt.compose.graphics.color.toComposeColor
import matt.compose.graphics.text.MyText
import matt.compose.state.rememberMutableStateOf
import matt.nn.deephys.calc.CategoryAccuracy
import matt.nn.deephys.calc.CategoryFalseNegativesSorted
import matt.nn.deephys.calc.CategoryFalsePositivesSorted
import matt.nn.deephys.gui.category.pie.CategoryPie
import matt.nn.deephys.gui.dataset.byimage.mult.MultipleImagesView
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.neuronListViewSwapper
import matt.nn.deephys.gui.global.DeephysLabel
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.global.tooltip.symbol.DeephysInfoSymbol
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.prim.str.addNewLinesUntilNumLinesIs
import matt.prim.str.elementsToString

@Composable
fun <A : Number> CategoryView(
    selection: CategorySelection,
    testLoader: TypedTestLike<A>,
    viewer: DatasetViewerState,
    settings: DeephysSettingsController,
    viewerWidth: Dp
) {
    Column {
        with(viewer.cacheContext) {
            DeephysLabel(
                selection.title.addNewLinesUntilNumLinesIs(3) /*so switching to confusion title with 3 lines isn't as jarring*/,
                font = titleBoldFont()
            )
            Column {


                when (selection) {
                    is Category          -> {
                        val acc =
                            CategoryAccuracy(
                                selection,
                                testLoader
                            )
                        with(testLoader.testRAMCache) {
                            DeephysLabel(
                                "Accuracy: ${
                                    acc.formatted()
                                }"
                            )
                        }

                        DeephysLabel("Category ID: ${selection.id}")
                    }


                    is CategoryConfusion -> {
                        with(testLoader.testRAMCache) {
                            DeephysLabel(
                                "Accuracy of ${selection.first.label}: ${
                                    CategoryAccuracy(
                                        selection.first,
                                        testLoader
                                    ).formatted()
                                }"
                            )
                            DeephysLabel(
                                "Accuracy of ${selection.second.label}: ${
                                    CategoryAccuracy(
                                        selection.second,
                                        testLoader
                                    ).formatted()
                                }"
                            )
                        }
                        DeephysLabel(
                            "Category IDs: ${
                                selection.allCategories.toList().map { it.id }.elementsToString()
                            }"
                        )
                    }
                }


                MyText(
                    buildString {
                        append("Neurons with highest average activation for ")
                        append(
                            when (selection) {
                                is Category          -> selection.label
                                is CategoryConfusion -> "${selection.first.label} and ${selection.second.label}"
                            }
                        )
                    },
                    font = subtitleFont()
                )

                neuronListViewSwapper(
                    viewer = viewer,
                    contents =
                        selection.allCategories.flatMapTo(mutableSetOf()) {
                            testLoader.test.imagesWithGroundTruth(
                                it
                            )
                        },
                    postDtypeTestLoader = testLoader.post,
                    fade = false /*I think issues are being causes since this child is fading while the parent is too*/,
                    settings = settings,
                    viewerWidth = viewerWidth
                )


                val allFalsePositives =
                    with(testLoader.testRAMCache) {
                        CategoryFalsePositivesSorted(
                            selection.primaryCategory,
                            testLoader
                        )()
                    }


                val shownFalsePositives =
                    when (selection) {
                        is Category          -> allFalsePositives
                        is CategoryConfusion -> allFalsePositives.filter { it.category == selection.second }
                    }


                val allFalseNegatives =
                    with(testLoader.testRAMCache) {
                        CategoryFalseNegativesSorted(selection.primaryCategory, testLoader)()
                    }
                val shownFalseNegatives =
                    when (selection) {
                        is Category          -> allFalseNegatives
                        is CategoryConfusion -> allFalseNegatives.filter { it.prediction == selection.second }
                    }
                if (viewer.showTutorials.value) {
                    DeephysInfoSymbol(
                        "Tip: Click the colored areas to navigate to the respective class. Shift-click it to analyze confusions with the currently selected class."
                    )
                }

                val nodeSize = rememberMutableStateOf<IntSize?>(null)
                Row(
                    horizontalArrangement =
                        Arrangement
                            .spacedBy(10.dp),
                    modifier =
                        Modifier.onSizeChanged {
                            nodeSize.value = it
                        }
                ) {
                    val cats = (testLoader.test.categories - selection.primaryCategory)
                    val cMap = Automatic().colorMap(cats.size)
                    val colorMap = cats.withIndex().associate { it.value to cMap[it.index]!!.toComposeColor() }
                    /*maxWidthProperty.bindWeakly(viewer.widthProperty*0.45)*/
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        /*maxWidthProperty.bindWeakly(viewer.widthProperty*0.45)*/
                        CategoryPie(
                            title = "False Positives (${allFalsePositives.size})",
                            cats,
                            nums =
                                cats.associateWith { cat ->
                                    allFalsePositives.filter { it.category == cat }.size
                                },
                            viewer,
                            colorMap = colorMap,
                            selected = (selection as? CategoryConfusion)?.second,
                            showAsList = viewer.showAsList1,
                            settings = settings
                        )
                        MultipleImagesView(
                            viewer = viewer,
                            images = shownFalsePositives,
                            title = null,
                            tooltip = CategoryFalsePositivesSorted.blurb,
                            fade = false,
                            settings = settings,
                            post = testLoader.post,
                            viewerWidth = viewerWidth
                        )
                    }


                    Canvas(Modifier.requiredWidth(10.dp)) {
                        val thePane = this
                        /*backgroundFill = FXColor(0.5, 0.5, 0.5, 0.2)


                    backgroundFill = FXColor(0.5, 0.5, 0.5, 0.2)*/
                        drawLine(
                            start =
                                Offset(
                                    x = (nodeSize.value!!.width / 2).toFloat(),
                                    y = 5f
                                ),
                            end =
                                Offset(
                                    x = (nodeSize.value!!.width / 2).toFloat(),
                                    y = nodeSize.value!!.height - 10f
                                ),
                            color =
                                FloatColor(
                                    0.5f,
                                    0.5f,
                                    0.5f,
                                    0.2f
                                ).toComposeColor(),
                            strokeWidth = 5f
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CategoryPie(
                            title = "False Negatives (${allFalseNegatives.size})",
                            cats,
                            nums =
                                cats.associateWith { cat ->
                                    allFalseNegatives.filter {
                                        it.prediction == cat
                                    }.size
                                },
                            viewer,
                            colorMap = colorMap,
                            selected = (selection as? CategoryConfusion)?.second,
                            showAsList = viewer.showAsList2,
                            settings = settings
                        )
                        MultipleImagesView(
                            viewer = viewer,
                            images = shownFalseNegatives,
                            title = null,
                            tooltip = CategoryFalseNegativesSorted.blurb,
                            fade = false,
                            settings = settings,
                            post = testLoader.post,
                            viewerWidth = viewerWidth
                        )
                    }
                    /*	v {
                      alignment = Pos.TOP_LEFT





                  deephysText("") {
          textAlignment = CENTER
          visibleAndManagedProp.bindWeakly(viewer.showTutorials)
        }



    }*/
                }
            }
        }
    }
}
