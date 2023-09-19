package matt.nn.deephys.gui.category

import javafx.geometry.Pos.TOP_CENTER
import matt.collect.set.contents.Contents
import matt.color.colorMap
import matt.fig.model.PieChartIrPlaceholder
import matt.fx.graphics.fxthread.runLater
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.line.line
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.pane
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.fx.graphics.wrapper.style.FXColor
import matt.fx.graphics.wrapper.style.toFXColor
import matt.fx.graphics.wrapper.textflow.textflow
import matt.nn.deephys.calc.CategoryAccuracy
import matt.nn.deephys.calc.CategoryFalseNegativesSorted
import matt.nn.deephys.calc.CategoryFalsePositivesSorted
import matt.nn.deephys.gui.category.pie.DeephysPieRenderer
import matt.nn.deephys.gui.dataset.byimage.mult.MultipleImagesView
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.neuronListViewSwapper
import matt.nn.deephys.gui.global.deephysLabel
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.global.tooltip.symbol.deephysInfoSymbol
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.obs.math.op.div
import matt.obs.math.op.minus
import matt.prim.str.addNewLinesUntilNumLinesIs
import matt.prim.str.elementsToString

class CategoryView<A : Number>(
    selection: CategorySelection,
    testLoader: TypedTestLike<A>,
    viewer: DatasetViewer,
    override val settings: DeephysSettingsController
) : VBoxWrapperImpl<RegionWrapper<*>>(), DeephysNode {



    init {
        with(viewer.cacheContext) {
            val memSafeSettings = settings

            deephysLabel(
                selection.title.addNewLinesUntilNumLinesIs(3) /*so switching to confusion title with 3 lines isn't as jarring*/
            ).titleBoldFont()



            v {


                when (selection) {
                    is Category          -> {
                        val acc = CategoryAccuracy(
                            selection, testLoader
                        )
                        deephysLabel(
                            "Accuracy: ${
                                acc.formatted()
                            }"
                        )
                        deephysLabel("Category ID: ${selection.id}")
                    }

                    is CategoryConfusion -> {
                        deephysLabel(
                            "Accuracy of ${selection.first.label}: ${
                                CategoryAccuracy(
                                    selection.first,
                                    testLoader
                                ).formatted()
                            }"
                        )
                        deephysLabel(
                            "Accuracy of ${selection.second.label}: ${
                                CategoryAccuracy(
                                    selection.second,
                                    testLoader
                                ).formatted()
                            }"
                        )
                        deephysLabel(
                            "Category IDs: ${
                                selection.allCategories.toList().map { it.id }.elementsToString()
                            }"
                        )
                    }
                }



                textflow<NW> {
                    deephysText("Neurons with highest average activation for ") {
                        subtitleFont()
                    }
                    deephysText(
                        when (selection) {
                            is Category          -> selection.label
                            is CategoryConfusion -> "${selection.first.label} and ${selection.second.label}"
                        }
                    ) {
                        subtitleFont()
                        //font = font.fixed().copy(weight = BOLD).fx()
                        //pointlesslyTryToSetTextFillWithoutAFlicker(Color.)
                    }
                }

                neuronListViewSwapper(
                    viewer = viewer,
                    contents = Contents(selection.allCategories.flatMap { testLoader.test.imagesWithGroundTruth(it) }),
                    fade = false /*I think issues are being causes since this child is fading while the parent is too*/,
                    settings = memSafeSettings
                )


                val allFalsePositives = with(testLoader.testRAMCache) {
                    CategoryFalsePositivesSorted(
                        selection.primaryCategory,
                        testLoader
                    )()
                }


                val shownFalsePositives = when (selection) {
                    is Category          -> allFalsePositives
                    is CategoryConfusion -> allFalsePositives.filter { it.category == selection.second }
                }


                val allFalseNegatives = with(testLoader.testRAMCache) {
                    CategoryFalseNegativesSorted(selection.primaryCategory, testLoader)()
                }
                val shownFalseNegatives = when (selection) {
                    is Category          -> allFalseNegatives
                    is CategoryConfusion -> allFalseNegatives.filter { it.prediction == selection.second }
                }
                deephysInfoSymbol("Tip: Click the colored areas to navigate to the respective class. Shift-click it to analyze confusions with the currently selected class.") {
                    visibleAndManagedProp.bindWeakly(viewer.showTutorials)
                }
                h {
                    isFillHeight = true
                    spacing = 10.0
                    val cats = (testLoader.test.categories - selection.primaryCategory)
                    val cMap = colorMap(cats.size)
                    val colorMap = cats.withIndex().associate { it.value to cMap[it.index]!!.toFXColor() }
                    /*maxWidthProperty.bindWeakly(viewer.widthProperty*0.45)*/
                    v {
                        alignment = TOP_CENTER
                        /*maxWidthProperty.bindWeakly(viewer.widthProperty*0.45)*/
                        +DeephysPieRenderer(
                            cats,
                            nums = cats.associateWith { cat ->
                                allFalsePositives.filter { it.category == cat }.size
                            },
                            viewer,
                            colorMap = colorMap,
                            selected = (selection as? CategoryConfusion)?.second,
                            showAsList = viewer.showAsList1,
                            settings = memSafeSettings
                        ).render(PieChartIrPlaceholder("False Positives (${allFalsePositives.size})"))
                        +MultipleImagesView(
                            viewer = viewer,
                            images = shownFalsePositives,
                            title = null,
                            tooltip = CategoryFalsePositivesSorted.blurb,
                            fade = false,
                            settings = memSafeSettings
                        )
                    }


                    pane<NW> {
                        val thePane = this
                        exactWidth = 10.0
                        /*backgroundFill = FXColor(0.5, 0.5, 0.5, 0.2)*/
                        //		  backgroundFill = FXColor.BLUE
                        //		  Platform.runLater {
                        /*backgroundFill = FXColor(0.5, 0.5, 0.5, 0.2)*/
                        //			backgroundFill = FXColor.BLUE
                        //		  }
                        //		  style = "-fx-background: blue"
                        line {
                            startY = 5.0
                            endYProperty.bind(thePane.heightProperty.minus(10.0))
                            /*fill = FXColor(0.5, 0.5, 0.5, 0.2)*/
                            fill = FXColor(0.5, 0.5, 0.5, 0.2)
                            stroke = FXColor(0.5, 0.5, 0.5, 0.2)
                            runLater {
                                fill = FXColor(0.5, 0.5, 0.5, 0.2)
                                stroke = FXColor(0.5, 0.5, 0.5, 0.2)
                            }
                            startXProperty.bind(thePane.widthProperty / 2)
                            endXProperty.bind(thePane.widthProperty / 2)
                            strokeWidth = 5.0
                        }
                    }

                    v {
                        alignment = TOP_CENTER
                        +DeephysPieRenderer(
                            cats,
                            nums = cats.associateWith { cat ->
                                allFalseNegatives.filter {
                                    it.prediction == cat
                                }.size
                            },
                            viewer,
                            colorMap = colorMap,
                            selected = (selection as? CategoryConfusion)?.second,
                            showAsList = viewer.showAsList2,
                            settings = memSafeSettings
                        ).render(PieChartIrPlaceholder("False Negatives (${allFalseNegatives.size})"))
                        +MultipleImagesView(
                            viewer = viewer,
                            images = shownFalseNegatives,
                            title = null,
                            tooltip = CategoryFalseNegativesSorted.blurb,
                            fade = false,
                            settings = memSafeSettings
                        )
                    }
                    /*	v {
                          alignment = Pos.TOP_LEFT

                          *//*  deephysText("") {
			  textAlignment = CENTER
			  visibleAndManagedProp.bindWeakly(viewer.showTutorials)
			}*//*
		}*/
                }


            }
        }
    }
}