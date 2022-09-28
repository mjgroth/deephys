package matt.nn.deephy.gui.category

import javafx.scene.Cursor
import javafx.scene.effect.Glow
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType.ROUND
import matt.hurricanefx.wrapper.line.arc.ArcWrapper
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.node.onHover
import matt.hurricanefx.wrapper.pane.PaneWrapperImpl
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.nn.deephy.calc.CategoryAccuracy
import matt.nn.deephy.calc.CategoryFalsePositivesSorted
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.global.deephyLabel
import matt.nn.deephy.gui.global.deephyText
import matt.nn.deephy.gui.global.deephyTooltip
import matt.nn.deephy.gui.global.subtitleFont
import matt.nn.deephy.gui.global.titleBoldFont
import matt.nn.deephy.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.load.test.TestLoader
import matt.nn.deephy.model.data.Category
import kotlin.math.cos
import kotlin.math.sin

class CategoryView(
  category: Category, testLoader: TestLoader, viewer: DatasetViewer
): VBoxWrapper<RegionWrapper<*>>() {
  init {
	deephyText(category.label).titleBoldFont()
	vbox<NodeWrapper> {
	  deephyText("Accuracy: ${CategoryAccuracy(category, testLoader)()}")
	  deephyText("False Positives").apply {
		subtitleFont()
		deephyTooltip(CategoryFalsePositivesSorted.blurb)
	  }
	  val falsePositives = CategoryFalsePositivesSorted(category, testLoader)()
	  hbox<NodeWrapper> {
		+ImageFlowPane(viewer).apply {
		  falsePositives.forEach {
			+DeephyImView(it, viewer)
		  }
		}


		val cats = (testLoader.awaitFinishedTest().categories - category)
		val hueStep = (1.0/cats.size)
		val colorMap = List(cats.size) {
		  val hue = it*hueStep*360
		  Color.hsb(hue, 0.5, 1.0)
		}

		val nums = cats.associateWith { cat ->
		  falsePositives.filter { it.category == cat }.size
		}
		val total = nums.values.sum().toDouble()


		+PaneWrapperImpl<Pane, NodeWrapper>(Pane()).apply {


		  exactWidth = 300.0
		  exactHeight = 300.0
		  var nextStart = 0.0
		  cats.filter { nums[it]!! > 0 }.mapIndexed { index, cat ->
			val ratio = nums[cat]!!/total
			val color = colorMap[index]

			val arcLength = ratio*360.0

			textflow<NodeWrapper> {
			  layoutX = 150.0 + 130*cos(-Math.toRadians(nextStart + arcLength/2.0))
			  layoutY = 150.0 + 130*sin(-Math.toRadians(nextStart + arcLength/2.0))

			  node.viewOrder = -1.0
			  val t = deephyLabel(cat.label)
			  backgroundFill = Color.BLACK

			  layoutY -= t.font.size
			}

			+ArcWrapper().apply {
			  centerX = 150.0
			  centerY = 150.0
			  radiusX = 100.0
			  radiusY = 100.0
			  length = arcLength
			  startAngle = nextStart
			  nextStart += arcLength
			  fill = color
			  stroke = color.invert()
			  node.apply {
				type = ROUND
			  }
			  cursor = Cursor.HAND
			  strokeWidth = 0.0
			  onHover {
				effect = if (it) Glow()
				else null

				strokeWidth = if (it) 2.0
				else 0.0

			  }
			  setOnMouseClicked {
				viewer.navigateTo(cat)
			  }
			}

		  }
		}
	  }
	  deephyText("Top Neurons").apply {
		subtitleFont()
		/*deephyTooltip(CategoryFalsePositivesSorted.blurb)*/

	  }




	}
  }
}

