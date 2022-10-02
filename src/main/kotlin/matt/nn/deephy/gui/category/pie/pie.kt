package matt.nn.deephy.gui.category.pie

import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.Glow
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType.ROUND
import matt.fx.graphics.wrapper.node.line.arc.ArcWrapper
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.node.onHover
import matt.fx.graphics.wrapper.pane.PaneWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.nn.deephy.gui.global.deephyLabel
import matt.nn.deephy.gui.global.deephyText
import matt.nn.deephy.gui.global.subtitleFont
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.data.Category
import kotlin.math.cos
import kotlin.math.sin

class CategoryPie(
  title: String,
  cats: List<Category>,
  nums: Map<Category, Int>,
  viewer: DatasetViewer,
  colorMap: Map<Category,Color>
): VBoxWrapperImpl<NodeWrapper>() {
  init {
	alignment = Pos.TOP_CENTER
	exactWidth = 350.0
	deephyText(title) {
	  subtitleFont()
	}
	val total = nums.values.sum().toDouble()
	+PaneWrapperImpl<Pane, NodeWrapper>(Pane()).apply {
	  exactWidth = 300.0
	  exactHeight = 300.0
	  var nextStart = 0.0
	  cats.filter { nums[it]!! > 0 }.mapIndexed { _, cat ->
		val ratio = nums[cat]!!/total
		val color = colorMap[cat]!!

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
}