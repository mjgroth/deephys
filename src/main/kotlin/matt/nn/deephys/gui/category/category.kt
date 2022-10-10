package matt.nn.deephys.gui.category

import javafx.scene.paint.Color
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.nn.deephys.calc.CategoryAccuracy
import matt.nn.deephys.calc.CategoryFalseNegativesSorted
import matt.nn.deephys.calc.CategoryFalsePositivesSorted
import matt.nn.deephys.calc.TopNeuronsCategory
import matt.nn.deephys.gui.category.pie.CategoryPie
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.NeuronListView
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.state.DeephySettings
import matt.obs.bind.binding
import matt.obs.math.op.times
import matt.prim.str.addNewLinesUntilNumLinesIs

class CategoryView(
  selection: CategorySelection, testLoader: TestLoader, viewer: DatasetViewer
): VBoxWrapperImpl<RegionWrapper<*>>() {
  init {


	deephyText(
	  selection.title.addNewLinesUntilNumLinesIs(3) /*so switching to confusion title with 3 lines isn't as jarring*/
	).titleBoldFont()
	vbox<NodeWrapper> {

	  when (selection) {
		is Category          -> deephyText(
		  "Accuracy: ${
			CategoryAccuracy(
			  selection, testLoader
			)()
		  }\n" /*added newline to make confusion selecting smoother*/
		)

		is CategoryConfusion -> {
		  deephyText(
			"Accuracy of ${selection.first.label}: ${CategoryAccuracy(selection.first, testLoader)()}"
		  )
		  deephyText(
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

	  hbox<NodeWrapper> {
		vbox<NodeWrapper> {
		  val maxIms = 25
		  deephyText("False Positives (${shownFalsePositives.size})").apply {
			subtitleFont()
			deephyTooltip(CategoryFalsePositivesSorted.blurb + "( first $maxIms)")
		  }

		  +ImageFlowPane(viewer).apply {
			prefWrapLengthProperty.bind(viewer.widthProperty*0.4)
			shownFalsePositives.take(maxIms).forEach {
			  +DeephyImView(it, viewer)
			}
		  }
		  deephyText("False Negatives (${shownFalseNegatives.size})").apply {
			subtitleFont()
			deephyTooltip(CategoryFalseNegativesSorted.blurb + "( first $maxIms)")
		  }
		  +ImageFlowPane(viewer).apply {
			prefWrapLengthProperty.bind(viewer.widthProperty*0.4)
			shownFalseNegatives.take(maxIms).forEach {
			  +DeephyImView(it, viewer)
			}
		  }
		}
		val cats = (testLoader.awaitFinishedTest().categories - selection.primaryCategory)
		val hueStep = (1.0/cats.size)
		val colorMap = List(cats.size) {
		  val hue = it*hueStep*360
		  Color.hsb(hue, 0.5, 1.0)
		}.withIndex().associate { cats[it.index] to it.value }
		+CategoryPie(
		  "False Positives",
		  cats,
		  nums = cats.associateWith { cat ->
			allFalsePositives.filter { it.category == cat }.size
		  },
		  viewer,
		  colorMap = colorMap,
		  selected = (selection as? CategoryConfusion)?.second
		)
		+CategoryPie(
		  "False Negatives",
		  cats,
		  nums = cats.associateWith { cat ->
			allFalseNegatives.filter {
			  it.prediction == cat
			  /*it.prediction() == cat*/
			}.size
		  },
		  viewer,
		  colorMap = colorMap,
		  selected = (selection as? CategoryConfusion)?.second
		)
	  }

	  /*TODO*/
	  /*deephyText("Top Neurons").apply {
		subtitleFont()
		matt.nn.deephys.gui.global.tooltip.deephyTooltip(CategoryFalsePositivesSorted.blurb)

	  }*/

	  swapper(viewer.layerSelection.binding(DeephySettings.normalizeTopNeuronActivations) {
		it?.to(DeephySettings.normalizeTopNeuronActivations.value)
	  }, "no top neurons") {
		NeuronListView(
		  viewer = viewer,
		  tops = TopNeuronsCategory(selection, this@swapper.first, this@swapper.second, testLoader),
		  normalized = this@swapper.second,
		  testLoader = testLoader
		)
	  }
	}
  }
}

