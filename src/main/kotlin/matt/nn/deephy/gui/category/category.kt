package matt.nn.deephy.gui.category

import javafx.scene.paint.Color
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.nn.deephy.calc.CategoryAccuracy
import matt.nn.deephy.calc.CategoryFalseNegativesSorted
import matt.nn.deephy.calc.CategoryFalsePositivesSorted
import matt.nn.deephy.calc.ImageTopPredictions
import matt.nn.deephy.gui.category.pie.CategoryPie
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.global.deephyText
import matt.nn.deephy.gui.global.deephyTooltip
import matt.nn.deephy.gui.global.subtitleFont
import matt.nn.deephy.gui.global.titleBoldFont
import matt.nn.deephy.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.data.Category
import matt.obs.bindings.math.times

class CategoryView(
  category: Category, testLoader: TestLoader, viewer: DatasetViewer
): VBoxWrapperImpl<RegionWrapper<*>>() {
  init {
	deephyText(category.label).titleBoldFont()
	vbox<NodeWrapper> {
	  deephyText("Accuracy: ${CategoryAccuracy(category, testLoader)()}")

	  val falsePositives = CategoryFalsePositivesSorted(category, testLoader)()
	  val falseNegatives = CategoryFalseNegativesSorted(category, testLoader)()
	  hbox<NodeWrapper> {
		vbox<NodeWrapper> {
		  val maxIms = 25
		  deephyText("False Positives").apply {
			subtitleFont()
			deephyTooltip(CategoryFalsePositivesSorted.blurb + "( first $maxIms)")
		  }

		  +ImageFlowPane(viewer).apply {
			prefWrapLengthProperty.bind(viewer.widthProperty*0.4)
			falsePositives.take(maxIms).forEach {
			  +DeephyImView(it, viewer)
			}
		  }
		  deephyText("False Negatives").apply {
			subtitleFont()
			deephyTooltip(CategoryFalseNegativesSorted.blurb + "( first $maxIms)")
		  }
		  +ImageFlowPane(viewer).apply {
			prefWrapLengthProperty.bind(viewer.widthProperty*0.4)
			falseNegatives.take(maxIms).forEach {
			  +DeephyImView(it, viewer)
			}
		  }
		}
		val cats = (testLoader.awaitFinishedTest().categories - category)
		val hueStep = (1.0/cats.size)
		val colorMap = List(cats.size) {
		  val hue = it*hueStep*360
		  Color.hsb(hue, 0.5, 1.0)
		}.withIndex().associate { cats[it.index] to it.value }
		+CategoryPie(
		  "False Positives",
		  cats,
		  nums = cats.associateWith { cat ->
			falsePositives.filter { it.category == cat }.size
		  },
		  viewer,
		  colorMap = colorMap
		)
		+CategoryPie(
		  "False Negatives",
		  cats,
		  nums = cats.associateWith { cat ->
			falseNegatives.filter { ImageTopPredictions(it, testLoader).findOrCompute().first().first == cat }.size
		  },
		  viewer,
		  colorMap = colorMap
		)
	  }

	  /*TODO*/
	  /*deephyText("Top Neurons").apply {
		subtitleFont()
		deephyTooltip(CategoryFalsePositivesSorted.blurb)

	  }*/


	}
  }
}

