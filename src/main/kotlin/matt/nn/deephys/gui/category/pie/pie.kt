package matt.nn.deephys.gui.category.pie

import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.ScrollPane.ScrollBarPolicy.ALWAYS
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.effect.Glow
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType.ROUND
import javafx.util.Duration
import matt.fx.control.wrapper.scroll.scrollpane
import matt.fx.graphics.anim.animation.keyframe
import matt.fx.graphics.anim.animation.timeline
import matt.fx.graphics.anim.interp.MyInterpolator
import matt.fx.graphics.style.DarkModeController
import matt.fx.graphics.style.background.backgroundFromColor
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.node.line.arc.ArcWrapper
import matt.fx.graphics.wrapper.node.shape.rect.RectangleWrapper
import matt.fx.graphics.wrapper.pane.PaneWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.textflow.TextFlowWrapper
import matt.fx.graphics.wrapper.textflow.textflow
import matt.fx.base.wrapper.obs.obsval.prop.toNonNullableProp
import matt.math.jmath.sigFigs
import matt.model.code.idea.MChartIdea
import matt.model.data.percent.Percent
import matt.nn.deephys.gui.global.deephyCheckbox
import matt.nn.deephys.gui.global.deephysLabel
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.obs.bind.binding
import matt.obs.bindings.bool.not
import matt.obs.bindings.bool.or
import matt.obs.bindings.str.mybuildobs.obsString
import matt.obs.prop.BindableProperty
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
  selected: Category? = null,
  private val showAsList: BindableProperty<Boolean>
): VBoxWrapperImpl<NodeWrapper>(), MChartIdea {

  companion object {
	private const val WIDTH = 300.0
	private const val HEIGHT = 300.0
	private const val CENTER_X = WIDTH/2.0
	private const val CENTER_Y = HEIGHT/2.0
	private const val MAX_SLICES = 25
	private const val ANIMATE = true
	private const val BAR_Y_INCR = 25.0
  }

  init {
	alignment = Pos.TOP_CENTER
	exactWidth = 350.0
	deephyCheckbox("show as list", showAsList)
	deephyText(title) {
	  subtitleFont()
	  veryLazyDeephysTooltip("only shows at most $MAX_SLICES slices (unless shown as list)")
	}
	val total = nums.values.sum().toDouble()
	scrollpane<NW> {
	  hbarPolicy = NEVER
	  isFitToWidth = true

	  vbarPolicyProperty.bind(this@CategoryPie.showAsList.binding {
		if (it) ALWAYS else NEVER
	  })

	  isFitToHeight = true
	  exactHeight = HEIGHT + 10.0

	  //	  fitToHeightProperty.bind(
	  //		this@CategoryPie.showAsList
	  //	  )


	  content = PaneWrapperImpl<Pane, NodeWrapper>(Pane()).apply {
		exactWidth = WIDTH
		val nonZeroCats = cats.filter { nums[it]!! > 0 }
		exactHeightProperty.bind(
		  this@CategoryPie.showAsList.binding {
			if (it) BAR_Y_INCR*nonZeroCats.size else HEIGHT
		  }
		)
		var nextStart = 0.0
		nonZeroCats.sortedBy { nums[it] }.reversed().mapIndexed { catIndex, cat ->
		  val ratio = nums[cat]!!/total
		  val percent = Percent(ratio*100)
		  val color = colorMap[cat]!!

		  val arcLength = ratio*360.0
		  val rads = -Math.toRadians(nextStart + arcLength/2.0)
		  val thetaX = cos(rads)
		  val thetaY = sin(rads)

		  val maxBarWidth = WIDTH - 50.0


		  val textXAddition = SimpleDoubleProperty(0.0)
		  val textYAddition = SimpleDoubleProperty(0.0)

		  val textXAdditionList = SimpleDoubleProperty(0.0)
		  val textYAdditionList = SimpleDoubleProperty(0.0)


		  val barY = BAR_Y_INCR*catIndex
		  val barWidth = ratio*maxBarWidth


		  textflow<NodeWrapper> {

			visibleAndManagedProp.bind(
			  this@CategoryPie.showAsList.or(catIndex < MAX_SLICES)
			)

			node.viewOrder = -1.0
			val t = deephysLabel(cat.label.truncateWithElipses(20)) {
			  textProperty.bind(obsString {
				append(this@CategoryPie.showAsList.binding {
				  if (it) "(${percent.percent.sigFigs(2)}%) " else ""
				})
				appendStatic(cat.label)
			  }.binding(this@CategoryPie.showAsList) {
				if (this@CategoryPie.showAsList.value) {
				  it.truncateWithElipses(30)
				} else it.truncateWithElipses(20)
			  })
			}

			layoutXProperty.bind(
			  this@CategoryPie.showAsList.binding(
				textXAddition.toNonNullableProp(),
				textXAdditionList.toNonNullableProp()
			  ) {
				(if (it) barWidth + 5.0 + textXAdditionList.value else CENTER_X + 130*thetaX + textXAddition.value)
			  }
			)



			layoutYProperty.bind(
			  this@CategoryPie.showAsList.binding(
				textYAddition.toNonNullableProp(),
				textYAdditionList.toNonNullableProp()
			  ) {
				(if (it) barY + textYAdditionList.value else CENTER_Y + 130*thetaY - t.font.size + textXAdditionList.value)
			  }
			)



			fun updateColor(textFlow: TextFlowWrapper<*>, isDarkMode: Boolean) {
			  textFlow.background = backgroundFromColor(if (isDarkMode) Color.BLACK else Color.WHITE)
			}
			updateColor(this, DarkModeController.darkModeProp.value)
			DarkModeController.darkModeProp.onChangeWithWeak(this) { tf, it ->
			  updateColor(tf, it)
			}


		  }


		  +CategoryBar(
			cat = cat,
			viewer = viewer,
			color = color,
			x = 0.0,
			y = barY,
			width = barWidth
		  ).apply {
			visibleAndManagedProp.bind(
			  this@CategoryPie.showAsList
			)
			highlighted.bind(hoverProperty)
			if (cat == selected) {
			  if (ANIMATE) timeline {
				keyframe(Duration.millis(500.0)) {
				  keyvalue(textXAdditionList, 25.0, MyInterpolator.EASE_OUT)
				  keyvalue(node.layoutXProperty(), layoutX + 25, MyInterpolator.EASE_OUT)
				}
			  } else {
				node.layoutX = layoutX + 25
			  }
			}
		  }

		  if (catIndex < MAX_SLICES) {

			+CategorySlice(
			  cat = cat,
			  viewer = viewer,
			  color = color,
			  arcLength = arcLength,
			  startAngle = nextStart
			).apply {
			  visibleAndManagedProp.bind(
				this@CategoryPie.showAsList.not()
			  )
			  highlighted.bind(hoverProperty)
			  if (cat == selected) {
				if (ANIMATE) timeline {
				  keyframe(Duration.millis(500.0)) {
					keyvalue(textXAddition, 25*thetaX, MyInterpolator.EASE_OUT)
					keyvalue(textYAddition, 25*thetaY, MyInterpolator.EASE_OUT)
					keyvalue(node.layoutXProperty(), layoutX + 25*thetaX, MyInterpolator.EASE_OUT)
					keyvalue(node.layoutYProperty(), layoutY + 25*thetaY, MyInterpolator.EASE_OUT)
				  }
				} else {
				  node.layoutX = layoutX + 25*thetaX
				  node.layoutY = layoutY + 25*thetaY
				}
			  }
			}
		  }


		  nextStart += arcLength

		}
	  }
	}


  }

  interface CategoryShape

  class CategoryBar(
	private val cat: Category,
	private val viewer: DatasetViewer,
	color: Color,
	y: Double,
	x: Double,
	width: Double
  ): RectangleWrapper(
	x = x,
	y = y,
	width = width,
	height = 25.0
  ), CategoryShape {


	fun click() {
	  viewer.navigateTo(cat)
	}

	fun shiftClick() {
	  viewer.navigateTo(CategoryConfusion(viewer.categorySelection.value!!.primaryCategory, cat))
	}


	init {
	  veryLazyDeephysTooltip(cat.label + " (shift-click for Confusion View)")
	  fill = color
	  stroke = color.invert()
	  cursor = Cursor.HAND
	  strokeWidth = 0.0
	  setOnMouseClicked {
		if (it.isShiftDown) shiftClick()
		else click()
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

  class CategorySlice(
	private val cat: Category,
	private val viewer: DatasetViewer,
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
  ), CategoryShape {


	fun click() {
	  viewer.navigateTo(cat)
	}

	fun shiftClick() {
	  viewer.navigateTo(CategoryConfusion(viewer.categorySelection.value!!.primaryCategory, cat))
	}


	init {
	  veryLazyDeephysTooltip(cat.label + " (shift-click for Confusion View)")
	  fill = color
	  stroke = color.invert()
	  node.apply {
		type = ROUND
	  }
	  cursor = Cursor.HAND
	  strokeWidth = 0.0
	  setOnMouseClicked {
		if (it.isShiftDown) shiftClick()
		else click()
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


