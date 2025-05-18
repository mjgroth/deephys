@file:Suppress("unused", "UnusedParameter")

package matt.nn.deephys.gui.category.pie

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import matt.compose.controls.desktop.scroll.MyVerticalScrollPane
import matt.compose.graphics.color.toComposeColor
import matt.compose.graphics.color.toMcolor
import matt.compose.state.shortcuts.rememberMutableStateOf
import matt.lang.anno.Duplicated
import matt.lang.common.unsafeError
import matt.math.numalg.precision.withPrecision
import matt.model.data.percent.Percent
import matt.nn.deephys.gui.global.DeephysLabel
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.obs.bind.binding
import matt.obs.bindings.bool.or
import matt.obs.bindings.str.mybuildobs.obsString
import matt.obs.prop.writable.BindableProperty
import matt.prim.str.truncateWithEllipses
import java.lang.ref.WeakReference
import kotlin.math.cos
import kotlin.math.sin

private val DEFAULT_CATEGORY = null

private object CategoryPieConstants {
    const val WIDTH = 300.0
    const val HEIGHT = 300.0
    const val CENTER_X = WIDTH / 2.0
    const val CENTER_Y = HEIGHT / 2.0
    const val MAX_SLICES = 25
    const val ANIMATE = true
    const val BAR_Y_INCR = 25.0
}
@Suppress("UnusedVariable", "UNUSED_VARIABLE")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPie(
    title: String,
    cats: List<Category>,
    nums: Map<Category, Int>,
    viewer: DatasetViewerState,
    colorMap: Map<Category, Color>,
    selected: Category? = DEFAULT_CATEGORY,
    showAsList: BindableProperty<Boolean>,
    settings: DeephysSettingsController
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.requiredWidth(350.0.dp)
    ) {

        unsafeError(
            """
            DeephyCheckbox("show as list", showAsList)    
            """.trimIndent()
        )



        DeephysTooltipArea(
            settings,
            "only shows at most ${CategoryPieConstants.MAX_SLICES} slices (unless shown as list)"
        ) {
            DeephysText(
                title,
                font = subtitleFont()
            )
        }

        val total = nums.values.sum().toDouble()



        MyVerticalScrollPane(
            showScrollBar = showAsList.value,
            modifier = Modifier.requiredHeight((CategoryPieConstants.HEIGHT + 10.0).dp)
        ) {
            val nonZeroCats = cats.filter { nums[it]!! > 0 }
            Box(
                Modifier
                    .requiredWidth(CategoryPieConstants.WIDTH.dp)
                    .requiredHeight(
                        showAsList.let {
                            if (it.value) CategoryPieConstants.BAR_Y_INCR * nonZeroCats.size else CategoryPieConstants.HEIGHT
                        }.dp
                    )
            ) {

                var nextStart = 0.0
                nonZeroCats.sortedBy { nums[it] }.reversed().mapIndexed { catIndex, cat ->
                    val ratio = nums[cat]!! / total
                    val percent = Percent(ratio * 100)
                    val color = colorMap[cat]!!

                    val arcLength = ratio * 360.0
                    val rads = -Math.toRadians(nextStart + arcLength / 2.0)
                    val thetaX = cos(rads)
                    val thetaY = sin(rads)

                    val maxBarWidth = CategoryPieConstants.WIDTH - 50.0


                    val textXAddition = mutableStateOf(0.0)
                    val textYAddition = mutableStateOf(0.0)

                    val textXAdditionList = mutableStateOf(0.0)
                    val textYAdditionList = mutableStateOf(0.0)


                    val barY = CategoryPieConstants.BAR_Y_INCR * catIndex
                    val barWidth = ratio * maxBarWidth


                    if (showAsList.or(catIndex < CategoryPieConstants.MAX_SLICES).value) {
                        FlowRow {
                            val t =
                                DeephysLabel(


                                    obsString {
                                        append(
                                            showAsList.binding {
                                                if (it) "(${percent.percent.withPrecision(2)}%) " else ""
                                            }
                                        )
                                        appendStatic(cat.label)
                                    }.binding(showAsList) {
                                        if (showAsList.value) it.truncateWithEllipses(30)
                                        else it.truncateWithEllipses(20)
                                    }.value

                                )
                            val weakText = WeakReference(t)

                            unsafeError(
                                """
                                        
                                layoutXProperty.bindWeakly(
                                    showAsList.binding(
                                        textXAddition.toNonNullableProp(),
                                        textXAdditionList.toNonNullableProp()
                                    ) {
                                        (if (it) barWidth + 5.0 + textXAdditionList.value else CategoryPieConstants.CENTER_X + 130 * thetaX + textXAddition.value)
                                    }
                                )



                                layoutYProperty.bindWeakly(
                                    showAsList.binding(
                                        textYAddition.toNonNullableProp(),
                                        textYAdditionList.toNonNullableProp()
                                    ) { sal ->
                                        weakText.get()?.let {
                                            (
                                                if (sal) barY + textYAdditionList.value
                                                else CategoryPieConstants.CENTER_Y + 130 * thetaY - it.font.size + textXAdditionList.value
                                            )
                                        } ?: -1.0
                                    }
                                )



                                fun updateColor(
                                    textFlow: TextFlowWrapper<*>,
                                    isDarkMode: Boolean
                                ) {
                                    textFlow.background =
                                        backgroundFromColor(if (isDarkMode) Color.BLACK else Color.WHITE)
                                }
                                    updateColor(this, DarkModeController.darkModeProp.value)
                                    DarkModeController.darkModeProp.onChangeWithWeak(this) { tf, it ->
                                        updateColor(tf, it)
                                    }        
                                """.trimIndent()
                            )
                        }
                    }


                    if (showAsList.value) {
                        CategoryBar(
                            cat = cat,
                            viewer = viewer,
                            color = color,
                            x = 0.0,
                            y = barY,
                            width = barWidth,
                            settings = settings
                        ).apply {
                            unsafeError(
                                """
                                highlighted.bind(hoverProperty)
                                if (cat == selected) {
                                    if (CategoryPieConstants.ANIMATE) timeline {
                                        keyframe(Duration.millis(500.0)) {
                                            keyvalue(textXAdditionList, 25.0, MyInterpolator.EASE_OUT)
                                            keyvalue(node.layoutXProperty(), layoutX + 25, MyInterpolator.EASE_OUT)
                                        }
                                    } else {
                                        node.layoutX = layoutX + 25
                                    }
                                }          
                                """.trimIndent()
                            )
                        }
                    }


                    if (catIndex < CategoryPieConstants.MAX_SLICES) {

                        if (!showAsList.value) {
                            CategorySlice(
                                cat = cat,
                                viewer = viewer,
                                color = color,
                                arcLength = arcLength,
                                startAngle = nextStart,
                                settings = settings
                            ).apply {
                                unsafeError(
                                    """
                                    highlighted.bind(hoverProperty)
                                    if (cat == selected) {
                                        if (CategoryPieConstants.ANIMATE) timeline {
                                            keyframe(Duration.millis(500.0)) {
                                                keyvalue(textXAddition, 25 * thetaX, MyInterpolator.EASE_OUT)
                                                keyvalue(textYAddition, 25 * thetaY, MyInterpolator.EASE_OUT)
                                                keyvalue(
                                                    node.layoutXProperty(),
                                                    layoutX + 25 * thetaX,
                                                    MyInterpolator.EASE_OUT
                                                )
                                                keyvalue(
                                                    node.layoutYProperty(),
                                                    layoutY + 25 * thetaY,
                                                    MyInterpolator.EASE_OUT
                                                )
                                            }
                                        } else {
                                            node.layoutX = layoutX + 25 * thetaX
                                            node.layoutY = layoutY + 25 * thetaY
                                        }
                                    }         
                                    """.trimIndent()
                                )
                            }
                        }
                    }


                    nextStart += arcLength
                }
            }
        }
    }
}



@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CategoryBar(
    cat: Category,
    viewer: DatasetViewerState,
    color: Color,
    y: Double,
    x: Double,
    width: Double,
    settings: DeephysSettingsController
) {

    val highlighted = rememberMutableStateOf(false)

    DeephysTooltipArea(
        settings,
        cat.label + " (shift-click for Confusion View)"
    ) {
        Box(
            Modifier.offset(
                x = x.dp,
                y = y.dp
            )
                .size(
                    width = width.dp,
                    height = 25.0.dp
                )
                .onPointerEvent(PointerEventType.Press) {
                    if (it.keyboardModifiers.isShiftPressed) {
                        viewer.navigateTo(CategoryConfusion(viewer.categorySelection.value!!.primaryCategory, cat))
                    } else {
                        viewer.navigateTo(cat)
                    }
                }
                .border(
                    width = if (highlighted.value) 2.0.dp else 0.0.dp,
                    color = color.toMcolor().invert().toComposeColor()
                ).pointerHoverIcon(PointerIcon.Hand)
        )
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Duplicated(23423439563)
fun CategorySlice(
    cat: Category,
    viewer: DatasetViewerState,
    color: Color,
    arcLength: Double,
    startAngle: Double,
    settings: DeephysSettingsController
) {

    val highlighted = rememberMutableStateOf(false)

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    val shape =
        GenericShape { size, layoutDir ->
            unsafeError(
                """
                                 draw arc with properties:
                                 
                                 
                                   centerX = CENTER_X,
                centerY = CENTER_Y,
                radiusX = 100.0,
                radiusY = 100.0,
                startAngle = startAngle,
                length = arcLength,
                type = ROUND
                                 
                                 
                """.trimIndent()
            )
        }
    DeephysTooltipArea(settings, cat.label + " (shift-click for Confusion View)") {
        Box(
            Modifier
                .background(
                    color = color,
                    shape = shape
                )
                .onPointerEvent(PointerEventType.Press) {
                    if (it.keyboardModifiers.isShiftPressed) {
                        viewer.navigateTo(CategoryConfusion(viewer.categorySelection.value!!.primaryCategory, cat))
                    } else {
                        viewer.navigateTo(cat)
                    }
                }
                .border(
                    shape = shape,
                    width = if (highlighted.value) 2.0.dp else 0.0.dp,
                    color = color.toMcolor().invert().toComposeColor()
                ).pointerHoverIcon(PointerIcon.Hand)
        )
    }
}

