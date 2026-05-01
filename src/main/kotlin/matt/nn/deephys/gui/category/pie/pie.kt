
package matt.nn.deephys.gui.category.pie

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import matt.compose.controls.desktop.scroll.verticallyScrollable
import matt.compose.controls.interaction.rememberMutableInteractionSource
import matt.compose.controls.mouse.j.handPointerIcon
import matt.compose.controls.mouse.j.onPointerPress
import matt.compose.graphics.background.defaultBackground
import matt.compose.graphics.color.toComposeColor
import matt.compose.graphics.color.toMcolor
import matt.compose.graphics.mods.thenIf
import matt.lang.anno.Duplicated
import matt.math.numalg.precision.withPrecision
import matt.model.data.percent.Percent
import matt.nn.deephys.gui.global.DeephyCheckbox
import matt.nn.deephys.gui.global.DeephysLabel
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.prim.str.truncateWithEllipses
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
@Composable
fun CategoryPie(
    title: String,
    cats: List<Category>,
    nums: Map<Category, Int>,
    viewer: DatasetViewerState,
    colorMap: Map<Category, Color>,
    @Suppress("unused") selected: Category? = DEFAULT_CATEGORY,
    showAsList: MutableState<Boolean>,
    settings: DeephysSettingsController
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.requiredWidth(350.0.dp)
    ) {

        DeephyCheckbox(s = "show as list", prop = showAsList)

        DeephysTooltipArea(
            settings,
            "only shows at most ${CategoryPieConstants.MAX_SLICES} slices (unless shown as list)"
        ) {
            DeephysText(
                s = title,
                style = subtitleFont()
            )
        }

        val total = nums.values.sum().toDouble()

        val nonZeroCats = cats.filter { nums[it]!! > 0 }
        Box(
            Modifier
                .requiredWidth(CategoryPieConstants.WIDTH.dp)
                .requiredHeight(
                    showAsList.let {
                        if (it.value) CategoryPieConstants.BAR_Y_INCR * nonZeroCats.size
                        else CategoryPieConstants.HEIGHT
                    }.dp
                )
                .thenIf(showAsList.value) {
                    Modifier.verticallyScrollable()
                }
        ) {

            var nextStart = 0.0
            nonZeroCats.sortedBy { nums[it] }.reversed().forEachIndexed { catIndex, cat ->
                val ratio = nums[cat]!! / total
                val percent = Percent(ratio * 100)
                val color = colorMap[cat]!!

                val arcLength = ratio * 360.0
                val rads = -Math.toRadians(nextStart + arcLength / 2.0)
                val thetaX = cos(rads)
                val thetaY = sin(rads)

                val maxBarWidth = CategoryPieConstants.WIDTH - 50.0

                val textXAddition = mutableDoubleStateOf(0.0)
                val textYAddition = mutableDoubleStateOf(0.0)

                val textXAdditionList = mutableDoubleStateOf(0.0)
                val textYAdditionList = mutableDoubleStateOf(0.0)

                val barY = CategoryPieConstants.BAR_Y_INCR * catIndex
                val barWidth = (ratio * maxBarWidth).dp

                if (showAsList.value || catIndex < CategoryPieConstants.MAX_SLICES) {
                    val s =
                        buildString {
                            append(
                                if (showAsList.value) "(${percent.percent.withPrecision(2)}%) " else ""
                            )
                            append(cat.label)
                        }.let {
                            if (showAsList.value) it.truncateWithEllipses(30)
                            else it.truncateWithEllipses(20)
                        }
                    val fontSize = LocalTextStyle.current.fontSize

                    FlowRow(
                        Modifier
                            .offset(
                                x = (
                                    if (showAsList.value) barWidth + 5.0.dp + textXAdditionList.value.dp
                                    else CategoryPieConstants.CENTER_X.dp + 130.dp * thetaX.toFloat() + textXAddition.value.dp
                                ),
                                y =
                                    (
                                        if (showAsList.value) barY.dp + textYAdditionList.value.dp
                                        else CategoryPieConstants.CENTER_Y.dp + 130.dp * thetaY.toFloat() -
                                            with(LocalDensity.current) { fontSize.toDp() } + textYAddition.value.dp
                                    )
                            )
                            .defaultBackground()
                    ) {
                        DeephysLabel(s, TextStyle(fontSize = fontSize))
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
                        settings = settings,
                        selected = cat == selected
                    )
                }

                if (catIndex < CategoryPieConstants.MAX_SLICES) {

                    if (!showAsList.value) {
                        CategorySlice(
                            cat = cat,
                            viewer = viewer,
                            color = color,
                            arcLength = arcLength,
                            startAngle = nextStart,
                            settings = settings,
                            selected = cat == selected,
                            thetaY = thetaY.toFloat(),
                            thetaX = thetaX.toFloat()
                        )
                    }
                }

                nextStart += arcLength
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
    width: Dp,
    settings: DeephysSettingsController,
    selected: Boolean
) {

    val interactionSource = rememberMutableInteractionSource()
    val hovered = interactionSource.collectIsHoveredAsState()
    val highlighted = hovered.value
    DeephysTooltipArea(
        settings,
        cat.label + " (shift-click for Confusion View)",
        modifier = Modifier.hoverable(interactionSource)
    ) {
        Box(
            Modifier.offset(
                x = x.dp,
                y = y.dp
            )
                .size(
                    width = width,
                    height = 25.0.dp
                )
                .onPointerPress {
                    if (it.keyboardModifiers.isShiftPressed) {
                        viewer.navigateTo(CategoryConfusion(viewer.boundCategory.value!!.primaryCategory, cat))
                    } else {
                        viewer.navigateTo(cat)
                    }
                }
                .border(
                    width = if (highlighted) 2.0.dp else 0.0.dp,
                    color = color.toMcolor().invert().toComposeColor()
                )
                .handPointerIcon()
                .offset(
                    x =
                        run {
                            val target =
                                if (selected) {
                                    25.dp
                                } else {
                                    0.dp
                                }
                            if (CategoryPieConstants.ANIMATE) {
                                animateDpAsState(target).value
                            } else {
                                target
                            }
                        }
                )
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
    settings: DeephysSettingsController,
    selected: Boolean,
    thetaX: Float,
    thetaY: Float
) {

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    val shape =
        GenericShape { size, _ ->
            addArc(
                oval =
                    Rect(
                        offset = Offset.Zero,
                        size = size
                    ),
                startAngleDegrees = startAngle.toFloat(),
                sweepAngleDegrees = arcLength.toFloat()
            )
        }
    val interactionSource = rememberMutableInteractionSource()
    val highlighted = interactionSource.collectIsHoveredAsState().value
    DeephysTooltipArea(settings, cat.label + " (shift-click for Confusion View)") {
        val offsetTarget =
            updateTransition(
                if (selected) {
                    DpOffset(x = 25.dp * thetaX, y = 25.dp * thetaY)
                } else {
                    DpOffset.Zero
                }
            )

        Box(
            Modifier
                .size(width = 200.0.dp, height = 200.0.dp)
                .offset(
                    x = CategoryPieConstants.CENTER_X.dp - 100.0.dp,
                    y = CategoryPieConstants.CENTER_Y.dp - 100.0.dp
                )
                .background(
                    color = color,
                    shape = shape
                )
                .onPointerPress {
                    if (it.keyboardModifiers.isShiftPressed) {
                        viewer.navigateTo(CategoryConfusion(viewer.boundCategory.value!!.primaryCategory, cat))
                    } else {
                        viewer.navigateTo(cat)
                    }
                }
                .border(
                    shape = shape,
                    width = if (highlighted) 2.0.dp else 0.0.dp,
                    color = color.toMcolor().invert().toComposeColor()
                )
                .handPointerIcon()
                .hoverable(interactionSource)
                .offset(
                    x =
                        if (CategoryPieConstants.ANIMATE) offsetTarget.animateDp { it.x }.value
                        else offsetTarget.targetState.x,
                    y =
                        if (CategoryPieConstants.ANIMATE) offsetTarget.animateDp { it.y }.value
                        else offsetTarget.targetState.y
                )
        )
    }
}
