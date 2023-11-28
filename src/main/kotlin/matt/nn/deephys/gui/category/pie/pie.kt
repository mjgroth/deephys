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
import matt.fig.modell.PieChartIrPlaceholder
import matt.fig.render.PieChartRenderer
import matt.fx.base.wrapper.obs.obsval.prop.toNonNullableProp
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
import matt.math.numalg.precision.withPrecision
import matt.model.data.percent.Percent
import matt.nn.deephys.gui.global.deephyCheckbox
import matt.nn.deephys.gui.global.deephysLabel
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.settings.DeephysSettingsController
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
import java.lang.ref.WeakReference
import kotlin.math.cos
import kotlin.math.sin

private val DEFAULT_CATEGORY = null

class DeephysPieRenderer(
    private val cats: List<Category>,
    private val nums: Map<Category, Int>,
    private val viewer: DatasetViewer,
    private val colorMap: Map<Category, Color>,
    private val selected: Category? = DEFAULT_CATEGORY,
    private val showAsList: BindableProperty<Boolean>,
    private val settings: DeephysSettingsController
) : PieChartRenderer<CategoryPie> {
    override fun render(figData: PieChartIrPlaceholder): CategoryPie {
        return CategoryPie(
            title = figData.title,
            cats = cats,
            nums = nums,
            viewer = viewer,
            colorMap = colorMap,
            selected = selected,
            showAsList = showAsList,
            settings = settings
        )
    }

}

class CategoryPie(
    title: String,
    cats: List<Category>,
    nums: Map<Category, Int>,
    viewer: DatasetViewer,
    colorMap: Map<Category, Color>,
    selected: Category? = DEFAULT_CATEGORY,
    showAsList: BindableProperty<Boolean>,
    settings: DeephysSettingsController
) : VBoxWrapperImpl<NodeWrapper>() {

    companion object {
        private const val WIDTH = 300.0
        private const val HEIGHT = 300.0
        private const val CENTER_X = WIDTH / 2.0
        private const val CENTER_Y = HEIGHT / 2.0
        private const val MAX_SLICES = 25
        private const val ANIMATE = true
        private const val BAR_Y_INCR = 25.0
    }

    init {
        val memSafeSettings = settings
        alignment = Pos.TOP_CENTER
        exactWidth = 350.0
        deephyCheckbox("show as list", showAsList, weakBothWays = true)
        deephysText(title) {
            subtitleFont()
            veryLazyDeephysTooltip(
                "only shows at most $MAX_SLICES slices (unless shown as list)",
                memSafeSettings
            )
        }
        val total = nums.values.sum().toDouble()
        scrollpane<NW> {
            hbarPolicy = NEVER
            isFitToWidth = true

            vbarPolicyProperty.bindWeakly(showAsList.binding {
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
                exactHeightProperty.bindWeakly(
                    showAsList.binding {
                        if (it) BAR_Y_INCR * nonZeroCats.size else HEIGHT
                    }
                )
                var nextStart = 0.0
                nonZeroCats.sortedBy { nums[it] }.reversed().mapIndexed { catIndex, cat ->
                    val ratio = nums[cat]!! / total
                    val percent = Percent(ratio * 100)
                    val color = colorMap[cat]!!

                    val arcLength = ratio * 360.0
                    val rads = -Math.toRadians(nextStart + arcLength / 2.0)
                    val thetaX = cos(rads)
                    val thetaY = sin(rads)

                    val maxBarWidth = WIDTH - 50.0


                    val textXAddition = SimpleDoubleProperty(0.0)
                    val textYAddition = SimpleDoubleProperty(0.0)

                    val textXAdditionList = SimpleDoubleProperty(0.0)
                    val textYAdditionList = SimpleDoubleProperty(0.0)


                    val barY = BAR_Y_INCR * catIndex
                    val barWidth = ratio * maxBarWidth


                    textflow<NodeWrapper> {

                        visibleAndManagedProp.bindWeakly(
                            showAsList.or(catIndex < MAX_SLICES)
                        )

                        node.viewOrder = -1.0
                        val t = deephysLabel(cat.label.truncateWithElipses(20)) {


                            textProperty.bindWeakly(


                                obsString {
                                    append(showAsList.binding {
                                        if (it) "(${percent.percent.withPrecision(2)}%) " else ""
                                    })
                                    appendStatic(cat.label)
                                }.binding(showAsList) {
                                    if (showAsList.value) it.truncateWithElipses(30)
                                    else it.truncateWithElipses(20)
                                }

                            )
                        }
                        val weakText = WeakReference(t)

                        layoutXProperty.bindWeakly(
                            showAsList.binding(
                                textXAddition.toNonNullableProp(),
                                textXAdditionList.toNonNullableProp()
                            ) {
                                (if (it) barWidth + 5.0 + textXAdditionList.value else CENTER_X + 130 * thetaX + textXAddition.value)
                            }
                        )



                        layoutYProperty.bindWeakly(
                            showAsList.binding(
                                textYAddition.toNonNullableProp(),
                                textYAdditionList.toNonNullableProp()
                            ) { sal ->
                                weakText.get()?.let {
                                    (if (sal) barY + textYAdditionList.value else CENTER_Y + 130 * thetaY - it.font.size + textXAdditionList.value)
                                } ?: -1.0
                            }
                        )



                        fun updateColor(
                            textFlow: TextFlowWrapper<*>,
                            isDarkMode: Boolean
                        ) {
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
                        width = barWidth,
                        settings = memSafeSettings
                    ).apply {
                        visibleAndManagedProp.bindWeakly(
                            showAsList
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
                            startAngle = nextStart,
                            settings = memSafeSettings
                        ).apply {
                            visibleAndManagedProp.bindWeakly(
                                showAsList.not()
                            )
                            highlighted.bind(hoverProperty)
                            if (cat == selected) {
                                if (ANIMATE) timeline {
                                    keyframe(Duration.millis(500.0)) {
                                        keyvalue(textXAddition, 25 * thetaX, MyInterpolator.EASE_OUT)
                                        keyvalue(textYAddition, 25 * thetaY, MyInterpolator.EASE_OUT)
                                        keyvalue(node.layoutXProperty(), layoutX + 25 * thetaX, MyInterpolator.EASE_OUT)
                                        keyvalue(node.layoutYProperty(), layoutY + 25 * thetaY, MyInterpolator.EASE_OUT)
                                    }
                                } else {
                                    node.layoutX = layoutX + 25 * thetaX
                                    node.layoutY = layoutY + 25 * thetaY
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
        width: Double,
        settings: DeephysSettingsController
    ) : RectangleWrapper(
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
            veryLazyDeephysTooltip(cat.label + " (shift-click for Confusion View)", settings)
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
        startAngle: Double,
        settings: DeephysSettingsController
    ) : ArcWrapper(
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
            veryLazyDeephysTooltip(cat.label + " (shift-click for Confusion View)", settings)
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


