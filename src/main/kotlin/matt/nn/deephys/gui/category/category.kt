package matt.nn.deephys.gui.category

import javafx.geometry.Pos
import javafx.scene.text.TextAlignment.CENTER
import matt.collect.set.contents.Contents
import matt.color.colorMap
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.node.visibleAndManagedWhen
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.fx.graphics.wrapper.style.toFXColor
import matt.nn.deephys.calc.CategoryAccuracy
import matt.nn.deephys.calc.CategoryFalseNegativesSorted
import matt.nn.deephys.calc.CategoryFalsePositivesSorted
import matt.nn.deephys.gui.category.pie.CategoryPie
import matt.nn.deephys.gui.dataset.byimage.mult.MultipleImagesView
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.neuronListViewSwapper
import matt.nn.deephys.gui.global.deephyLabel
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.nn.deephys.model.data.CategorySelection
import matt.prim.str.addNewLinesUntilNumLinesIs

class CategoryView(
  selection: CategorySelection,
  testLoader: TestLoader,
  viewer: DatasetViewer
): VBoxWrapperImpl<RegionWrapper<*>>() {
  init {


	deephyLabel(
	  selection.title.addNewLinesUntilNumLinesIs(3) /*so switching to confusion title with 3 lines isn't as jarring*/
	).titleBoldFont()
	vbox<NodeWrapper> {

	  when (selection) {
		is Category          -> deephyLabel(
		  "Accuracy: ${
			CategoryAccuracy(
			  selection, testLoader
			)()
		  }\n" /*added newline to make confusion selecting smoother*/
		)

		is CategoryConfusion -> {
		  deephyLabel(
			"Accuracy of ${selection.first.label}: ${CategoryAccuracy(selection.first, testLoader)()}"
		  )
		  deephyLabel(
			"Accuracy of ${selection.second.label}: ${CategoryAccuracy(selection.second, testLoader)()}"
		  )
		}
	  }


	  val allFalsePositives = CategoryFalsePositivesSorted(selection.primaryCategory, testLoader)()


	  val shownFalsePositives = when (selection) {
		is Category          -> allFalsePositives
		is CategoryConfusion -> allFalsePositives.filter { it.category == selection.second }
	  }


	  val allFalseNegatives = CategoryFalseNegativesSorted(selection.primaryCategory, testLoader)()

	  val shownFalseNegatives = when (selection) {
		is Category          -> allFalseNegatives
		is CategoryConfusion -> allFalseNegatives.filter { it.prediction == selection.second }
	  }

	  h {
		v {
		  +MultipleImagesView(
			viewer = viewer,
			images = shownFalsePositives,
			title = "False Positives",
			tooltip = CategoryFalsePositivesSorted.blurb
		  )
		  +MultipleImagesView(
			viewer = viewer,
			images = shownFalseNegatives,
			title = "False Negatives",
			tooltip = CategoryFalseNegativesSorted.blurb
		  )
		}
		v {

		  alignment = Pos.CENTER

		  h {
			val cats = (testLoader.awaitFinishedTest().categories - selection.primaryCategory)
			val cMap = colorMap(cats.size)
			val colorMap = cats.withIndex().associate { it.value to cMap[it.index]!!.toFXColor() }
			+CategoryPie(
			  "False Positives",
			  cats,
			  nums = cats.associateWith { cat ->
				allFalsePositives.filter { it.category == cat }.size
			  },
			  viewer,
			  colorMap = colorMap,
			  selected = (selection as? CategoryConfusion)?.second,
			  showAsList = viewer.showAsList1
			)
			+CategoryPie(
			  "False Negatives",
			  cats,
			  nums = cats.associateWith { cat ->
				allFalseNegatives.filter {
				  it.prediction == cat
				}.size
			  },
			  viewer,
			  colorMap = colorMap,
			  selected = (selection as? CategoryConfusion)?.second,
			  showAsList = viewer.showAsList2
			)

		  }
		  deephyText("tip: click colored areas to navigate to the class. Shift-click to analyze confusions with the current class.") {
			textAlignment = CENTER
			visibleAndManagedWhen {
			  viewer.showTutorials
			}
		  }

		}


	  }

	  val topNeuronsLabel = when (selection) {
		is Category          -> "top neurons according to their average activation for ${selection.label}"
		is CategoryConfusion -> "top neurons according to their average activation for ${selection.first} and ${selection.second}"
	  }
	  deephyLabel(topNeuronsLabel) {
		subtitleFont()
	  }
	  neuronListViewSwapper(
		viewer = viewer,
		contents = Contents(
		  selection.allCategories.flatMap { testLoader.awaitFinishedTest().imagesWithGroundTruth(it) }
		)
	  )
	}
  }
}