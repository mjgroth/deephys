package matt.nn.deephys.gui.category.pie

import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.Glow
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType.ROUND
import javafx.util.Duration
import matt.fx.graphics.anim.animation.keyframe
import matt.fx.graphics.anim.animation.timeline
import matt.fx.graphics.anim.interp.MyInterpolator
import matt.fx.graphics.style.DarkModeController
import matt.fx.graphics.style.backgroundFromColor
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.node.line.arc.ArcWrapper
import matt.fx.graphics.wrapper.pane.PaneWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.textflow.TextFlowWrapper
import matt.fx.graphics.wrapper.textflow.textflow
import matt.model.code.idea.MChartIdea
import matt.nn.deephys.gui.global.deephyLabel
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.obs.prop.VarProp
import matt.prim.str.truncateWithElipses
import kotlin.math.cos
import kotlin.math.sin

class CategoryPie(
  title: String,
  cats: List<Category>,
  nums: Map<Category, Int>,
  viewer: DatasetViewer,
  colorMap: Map<Category, Color>,
  selected: Category? = null
): VBoxWrapperImpl<NodeWrapper>(), MChartIdea {

  companion object {
	const val CENTER_X = 150.0
	const val CENTER_Y = 150.0
	const val MAX_SLICES = 25
  }

  init {
	alignment = Pos.TOP_CENTER
	exactWidth = 350.0
	deephyText(title) {
	  subtitleFont()
	  deephyTooltip("only shows at most ${MAX_SLICES} slices")
	}
	val total = nums.values.sum().toDouble()
	+PaneWrapperImpl<Pane, NodeWrapper>(Pane()).apply {
	  exactWidth = 300.0
	  exactHeight = 300.0
	  var nextStart = 0.0
	  cats.filter { nums[it]!! > 0 }.sortedBy { nums[it] }.take(MAX_SLICES).mapIndexed { _, cat ->
		val ratio = nums[cat]!!/total
		val color = colorMap[cat]!!

		val arcLength = ratio*360.0

		val rads = -Math.toRadians(nextStart + arcLength/2.0)
		val thetaX = cos(rads)
		val thetaY = sin(rads)

		val theLabel = textflow<NodeWrapper> {

		  layoutX = CENTER_X + 130*thetaX
		  layoutY = CENTER_Y + 130*thetaY

		  node.viewOrder = -1.0
		  val t = deephyLabel(cat.label.truncateWithElipses(20))
		  fun updateColor(textFlow: TextFlowWrapper<*>, isDarkMode: Boolean) {
			textFlow.background = backgroundFromColor(if (isDarkMode) Color.BLACK else Color.WHITE)
		  }
		  updateColor(this, DarkModeController.darkModeProp.value)
		  DarkModeController.darkModeProp.onChangeWithWeak(this) { tf, it ->
			updateColor(tf, it)
		  }

		  layoutY -= t.font.size
		}

		+CategorySlice(
		  cat = cat,
		  viewer = viewer,
		  color = color,
		  arcLength = arcLength,
		  startAngle = nextStart
		).apply {
		  highlighted.bind(hoverProperty)
		  if (cat == selected) {
			timeline {
			  keyframe(Duration.millis(500.0)) {
				keyvalue(theLabel.node.layoutXProperty(), theLabel.layoutX + 25*thetaX, MyInterpolator.EASE_OUT)
				keyvalue(theLabel.node.layoutYProperty(), theLabel.layoutY + 25*thetaY, MyInterpolator.EASE_OUT)
				keyvalue(node.layoutXProperty(), layoutX + 25*thetaX, MyInterpolator.EASE_OUT)
				keyvalue(node.layoutYProperty(), layoutY + 25*thetaY, MyInterpolator.EASE_OUT)
			  }
			}
		  }
		}
		nextStart += arcLength

	  }
	}
  }

  private class CategorySlice(
	cat: Category,
	viewer: DatasetViewer,
	color: Color,
	arcLength: Double,
	startAngle: Double
  ): ArcWrapper(
	centerX = CENTER_X,
	centerY = CENTER_Y,
	radiusX = 100.0,
	radiusY = 100.0,
	startAngle = startAngle,
	length = arcLength
  ) {
	init {
	  deephyTooltip(cat.label + " (shift-click for Confusion View)")
	  fill = color
	  stroke = color.invert()
	  node.apply {
		type = ROUND
	  }
	  cursor = Cursor.HAND
	  strokeWidth = 0.0
	  setOnMouseClicked {
		if (it.isShiftDown) {
		  viewer.navigateTo(CategoryConfusion(viewer.categorySelection.value!!.primaryCategory, cat))
		} else {
		  viewer.navigateTo(cat)
		}
	  }

	}

	val highlighted = VarProp(false).apply {
	  onChange {
		effect = if (it) Glow()
		else null

		strokeWidth = if (it) 2.0
		else 0.0
	  }
	}


  }
}


