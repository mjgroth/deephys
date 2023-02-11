package matt.nn.deephys.gui.category

import javafx.geometry.Pos
import javafx.scene.text.TextAlignment.CENTER
import matt.collect.set.contents.Contents
import matt.color.colorMap
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.fx.graphics.wrapper.style.toFXColor
import matt.nn.deephys.calc.CategoryAccuracy
import matt.nn.deephys.calc.CategoryFalseNegativesSorted
import matt.nn.deephys.calc.CategoryFalsePositivesSorted
import matt.nn.deephys.gui.category.pie.CategoryPie
import matt.nn.deephys.gui.dataset.byimage.mult.MultipleImagesView
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.neuronListViewSwapper
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.deephysLabel
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.obs.math.double.op.times
import matt.prim.str.addNewLinesUntilNumLinesIs
import matt.prim.str.elementsToString

class CategoryView<A: Number>(
  selection: CategorySelection,
  testLoader: TypedTestLike<A>,
  viewer: DatasetViewer,
  override val settings: DeephysSettingsController
): VBoxWrapperImpl<RegionWrapper<*>>(), DeephysNode {



  init {

	val memSafeSettings = settings

	deephysLabel(
	  selection.title.addNewLinesUntilNumLinesIs(3) /*so switching to confusion title with 3 lines isn't as jarring*/
	).titleBoldFont()



	v {


	  when (selection) {
		is Category          -> {
		  deephysLabel(
			"Accuracy: ${
			  CategoryAccuracy(
				selection, testLoader
			  )()
			}\n" /*added newline to make confusion selecting smoother*/
		  )
		  deephysLabel("Category ID: ${selection.id}")
		}

		is CategoryConfusion -> {
		  deephysLabel(
			"Accuracy of ${selection.first.label}: ${CategoryAccuracy(selection.first, testLoader)()}"
		  )
		  deephysLabel(
			"Accuracy of ${selection.second.label}: ${CategoryAccuracy(selection.second, testLoader)()}"
		  )
		  deephysLabel("Category IDs: ${selection.allCategories.toList().map { it.id }.elementsToString()}")
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
		  //		  println("width1@${this.hashCode()}=$width,max=${maxWidth}")
		  //		  widthProperty.onChange {
		  //			println("width2@${this.hashCode()}=$width,max=${maxWidth}")
		  //		  }
		  //		  maxWidthProperty.onChange {
		  //			println("width2@${this.hashCode()}=$width,max=${maxWidth}")
		  //		  }
		  maxWidthProperty.bindWeakly(viewer.stage!!.widthProperty*0.45)
		  +MultipleImagesView(
			viewer = viewer,
			images = shownFalsePositives,
			title = "False Positives",
			tooltip = CategoryFalsePositivesSorted.blurb,
			fade = false,
			settings = memSafeSettings
		  )
		  +MultipleImagesView(
			viewer = viewer,
			images = shownFalseNegatives,
			title = "False Negatives",
			tooltip = CategoryFalseNegativesSorted.blurb,
			fade = false,
			settings = memSafeSettings
		  )

		}
		v {

		  //		  minWidthProperty.bindWeakly(viewer.widthProperty*0.45)

		  alignment = Pos.CENTER

		  h {
			val cats = (testLoader.test.categories - selection.primaryCategory)
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
			  showAsList = viewer.showAsList1,
			  settings = memSafeSettings
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
			  showAsList = viewer.showAsList2,
			  settings = memSafeSettings
			)
			//			println("ADDED CAT PIES")

		  }
		  deephysText("Tip: Click the colored areas to navigate to the respective class. Shift-click it to analyze confusions with the currently selected class.") {
			textAlignment = CENTER
			visibleAndManagedProp.bindWeakly(viewer.showTutorials)
		  }

		}
	  }


	  val topNeuronsLabel = when (selection) {
		is Category          -> "top neurons according to their average activation for ${selection.label}"
		is CategoryConfusion -> "top neurons according to their average activation for ${selection.first} and ${selection.second}"
	  }
	  deephysLabel(topNeuronsLabel) {
		subtitleFont()
	  }
	  neuronListViewSwapper(
		viewer = viewer,
		contents = Contents(selection.allCategories.flatMap { testLoader.test.imagesWithGroundTruth(it) }),
		fade = false /*I think issues are being causes since this child is fading while the parent is too*/,
		settings = memSafeSettings
	  )
	}
  }
}