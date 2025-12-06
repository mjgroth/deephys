package matt.nn.deephys.gui.global

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import matt.color.colors.Colors
import matt.compose.controls.buttons.MyButton
import matt.compose.controls.check.MyCheckbox
import matt.compose.controls.click.ActionText
import matt.compose.controls.composers.action.common.ActionButton
import matt.compose.controls.icon.MyClickableIcon
import matt.compose.controls.spinner.MyNumberSpinner
import matt.compose.controls.text.MyClickableText
import matt.compose.controls.textfields.parsing.parser.SimpleBiTextParser
import matt.compose.controls.textfields.parsing.parser.filtered
import matt.compose.graphics.Compose
import matt.compose.graphics.color.toComposeColor
import matt.compose.graphics.layout.AlignedRow
import matt.compose.graphics.text.MyText
import matt.compose.graphics.text.style.style.localEm
import matt.compose.state.lang.ALWAYS_TRUE
import matt.compose.state.prop.rememberBoundComposeState
import matt.compose.state.readonly.LAZY_STATE_PROBLEM_ALT
import matt.compose.state.readonly.LAZY_STATE_PROBLEM_REASON
import matt.compose.state.shortcuts.rememberMutableStateOf
import matt.compose.state.statefulmodel.action.SimpleAction
import matt.compose.state.toggle.NewToggleMechanism
import matt.lang.anno.Alert
import matt.lang.anno.CodeAlertCategory.TechnicalIssue
import matt.lang.anno.optin.UnsafeMattCode
import matt.lang.cfnf.FailureInfo
import matt.lang.common.unsafeError
import matt.lang.function.Op
import matt.lang.generic.GenericFailable
import matt.lang.generic.isFailure
import matt.lang.generic.onFailure
import matt.log.warn.common.warn
import matt.math.numalg.precision.withPrecision
import matt.nn.deephys.gui.global.color.DeephysPalette
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.obs.bindings.str.ObsS
import matt.obs.prop.writable.Var

/*null because gets in the way of existing animations for pie slices
val DEEPHYS_FADE_DUR = 500.milliseconds*/
val DEEPHYS_FADE_DUR = null

@Suppress("UnusedVariable", "UNUSED_VARIABLE", "UnusedParameter", "unused")
@Composable
inline fun <reified E : Comparable<E>> DeephysSpinner(
    selected: MutableState<E>,
    label: String,
    choices: List<E>,
    crossinline defaultChoice: () -> E,
    converter: SimpleBiTextParser<E>,
    viewer: DatasetViewerState,
    getCurrent: State<E?>,
    crossinline acceptIf: (E) -> Boolean,
    crossinline navAction: DatasetViewerState.(E) -> Unit
) {
    AlignedRow {
        val sortedChoices = choices.sorted()
        val valueOrError = rememberMutableStateOf<GenericFailable<E, FailureInfo>> { GenericFailable.success(selected.value) }
        if (viewer.isUnboundToDSet.value) {
            DeephysLabeledControl(label) {
                MyNumberSpinner(
                    value = selected.value,
                    range = sortedChoices.first()..sortedChoices.last(),
                    enableScroll = false,
                    baseTypeParser =
                        converter.filtered {
                            it in sortedChoices
                        },
                    modifier =
                        Modifier.then(
                            if (valueOrError.value.isFailure()) {
                                Modifier.background(Colors.Red.toComposeColor())
                            } else {
                                Modifier
                            }
                        ),
                    buttonStepUp = {
                        val oldIndex = sortedChoices.indexOf(it)
                        sortedChoices[
                            if (oldIndex == sortedChoices.lastIndex) {
                                0
                            } else {
                                oldIndex + 1
                            }
                        ]
                    },
                    buttonStepDown = {
                        val oldIndex = sortedChoices.indexOf(it)
                        sortedChoices[
                            if (oldIndex == 0) {
                                sortedChoices.lastIndex
                            } else {
                                oldIndex - 1
                            }
                        ]
                    },
                    onValueChange = {
                        val real =
                            if (acceptIf(it)) {
                                it
                            } else {
                                defaultChoice()
                            }
                        viewer.navAction(real)
                        selected.value = real
                        valueOrError.value = GenericFailable.success(real)
                    },
                    onFailedUpdate = {
                        valueOrError.value = GenericFailable.failure(it)
                    }
                )
            }
        }
        val result = valueOrError.value
        result.onFailure {
            DeephysText(s = "please input valid integer index between 0 and ${choices.size}")
        }
    }
}

@Composable
fun DeephysLabeledControl(
    label: String,
    control: Compose
) = AlignedRow {
    AlignedRow(
        Modifier.width(60.dp)
    ) {
        DeephysText(s = "$label:")
    }
    Box(
        Modifier.width(100.dp),
        propagateMinConstraints = true
    ) {
        control()
    }
}

@Composable
fun DeephysLabeledControl2(
    label: String,
    control: Compose
) = AlignedRow {
    AlignedRow(Modifier.width(60.dp)) {
        DeephysText(s = "$label:")
    }
    Spacer(Modifier.width(5.dp))
    Box(Modifier.width(500.dp), propagateMinConstraints = true) {
        control()
    }
}

@UnsafeMattCode(saferAlternative = LAZY_STATE_PROBLEM_ALT, reason = LAZY_STATE_PROBLEM_REASON)
@Composable
fun DeephysText(
    s: ObsS,
    font: FontFamily = DEEPHYS_FONT_DEFAULT
) {
    DeephysText(s = s.rememberBoundComposeState().value, font = font)
}

@UnsafeMattCode(saferAlternative = LAZY_STATE_PROBLEM_ALT, reason = LAZY_STATE_PROBLEM_REASON)
@Composable
fun DeephysText(
    s: ObsS,
    style: TextStyle
) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current + TextStyle(fontFamily = DEEPHYS_FONT_DEFAULT) + style
    ) {
        MyText(s.rememberBoundComposeState().value)
    }
}

@Composable
fun DeephysText(
    modifier: Modifier = Modifier,
    s: String = "",
    font: FontFamily = DEEPHYS_FONT_DEFAULT
) {
    MyText(s, font = font, modifier = modifier)
}

@Composable
fun DeephysText(
    modifier: Modifier = Modifier,
    s: String = "",
    style: TextStyle
) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current + TextStyle(fontFamily = DEEPHYS_FONT_DEFAULT) + style
    ) {
        MyText(s, modifier = modifier)
    }
}
@Composable
fun SigFigText(
    num: Number,
    sigFigSett: State<Int>,
    numSuffix: String,
    settings: DeephysSettingsController,
    tooltip: String
) = DeephysTooltipArea(settings, tooltip) {
    val sett = sigFigSett.value
    DeephysText(
        s =
            when (num) {
                is Float  -> num.withPrecision(sett).toString()
                is Double -> num.withPrecision(sett).toString()
                else      -> error("not ready for different dtype")
            } + numSuffix
    )
}

@Composable
fun subtitleFont() = DEEPHY_FONT_SUBTITLE
@Composable
fun titleFont() = DEEPHY_FONT_TITLE
@Composable
fun titleBoldFont() = DEEPHY_FONT_TITLE_BOLD

@Composable
fun DeephyActionText(
    s: String = "",
    op: () -> Unit
) = ActionText(
    s,
    font = DEEPHYS_FONT_DEFAULT
) {
    op()
}

@Composable
fun DeephyActionLabel(
    s: String = "",
    font: FontFamily = DEEPHYS_FONT_DEFAULT,
    op: () -> Unit
) = ActionText(
    s,
    font = font
) {
    op()
}
@Composable
fun DeephyActionLabel(
    s: String = "",
    style: TextStyle,
    op: () -> Unit
) = CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current + style) {
    ActionText(
        s
    ) {
        op()
    }
}

@Composable
fun DeephysLabel(
    s: String = "",
    font: FontFamily = DEEPHYS_FONT_DEFAULT
) {
    MyText(s, font = font)
}
@Composable
fun DeephysLabel(
    s: String = "",
    style: TextStyle
) {
    CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current + style) {
        MyText(s)
    }
}

@Composable
fun DeephyHyperlink(
    s: String = "",
    action: Op
) = MyClickableText(s, font = DEEPHYS_FONT_DEFAULT) {
    action()
}

@UnsafeMattCode(saferAlternative = LAZY_STATE_PROBLEM_ALT, reason = LAZY_STATE_PROBLEM_REASON)
@Composable
fun DeephyCheckbox(
    modifier: Modifier = Modifier,
    s: String = "",
    prop: Var<Boolean>? = null
) = MyCheckbox(
    label = s,
    modifier = modifier,
    checked = prop!!.rememberBoundComposeState() as MutableState
)

@Composable
fun DeephyButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    s: String = "",
    action: () -> Unit
) = MyButton(
    s,
    font = DEEPHYS_FONT_DEFAULT,
    modifier = modifier,
    enabled = enabled
) {
    action()
}

@Composable
fun DeephyButton(
    icon: ImageVector,
    action: () -> Unit
) = MyClickableIcon(
    icon,
    tint = DeephysPalette.deephysBlue1
) {
    action()
}

@Suppress("unused")
private const val DEEPHY_ICON_BUTTON_SIZE = 25

@Suppress("UnusedParameter", "unused")
@Composable
fun DeephyIconButton(
    icon: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    action: () -> Unit
) {
    warn("icon here comes from resource files, and might need to have \".svg \" appended to it")
    warn("graphic = svgIcon(icon, DEEPHY_ICON_BUTTON_SIZE)")
    warn(
        """
                  hoverColor = FloatColor(0.5f, 0.5f, 0.5f, 0.2f).toComposeColor(),
        clickColor = FloatColor(1.0f, 1.0f, 0.0f, 0.5f).toComposeColor()
        """.trimIndent()
    )
    DeephyButton(
        modifier = modifier,
        enabled = enabled,
        s = "ICON HERE"
    ) {
        action()
    }
}

@Suppress("unused")
@Alert(TechnicalIssue, "I guess this is supposed to look like a radio button? Or should it also behave different from a toggle button?")
@Composable
fun <V : Any> DeephyRadioButton(
    s: String,
    group: NewToggleMechanism<V>,
    value: V
) = DeephyToggleButton(s, value, group)

@Composable
fun <V : Any> DeephyToggleButton(
    s: String = "",
    value: V,
    group: NewToggleMechanism<V>
) = MyButton(s, enabled = group.selected.value != value) {
    group.selected.value = value
}

@Composable
fun DeephyActionButton(
    s: String = "",
    action: () -> Unit
) = ActionButton(
    action =
        SimpleAction(
            name = s,
            enabled = ALWAYS_TRUE,
            op = {
                action()
            }
        ),
    text = s
)

@Suppress("unused")
fun deephysSingleCharButtonFont() {
    unsafeError(
        """
        font.fixed().copy(size = DEEPHYS_SINGLE_CHAR_BUTTON_FONT_SIZE).fx()    
        """.trimIndent()
    )
}

@Suppress("unused")
const val DEEPHYS_SINGLE_CHAR_BUTTON_FONT_SIZE = 18.0

const val DEEPHYS_LATEX_TOOLTIP_SCALE = 0.70

@OptIn(ExperimentalTextApi::class)
val DEEPHYS_FONT_DEFAULT: FontFamily by lazy {
    FontFamily("Georgia")
}
@get:Composable
val DEEPHY_FONT_SUBTITLE get() =
    TextStyle(
        fontFamily = DEEPHYS_FONT_DEFAULT,
        fontSize = 1.2.localEm()
    )
@get:Composable
val DEEPHY_FONT_TITLE get() =
    DEEPHY_FONT_SUBTITLE.copy(
        fontSize = 1.5.localEm()
    )
@get:Composable
val DEEPHY_FONT_TITLE_BOLD get() =
    DEEPHY_FONT_TITLE.copy(
        fontWeight = FontWeight.Bold
    )

@Composable
fun DeephysNullMessageFact(message: String) {
    Column {
        SpacerWithOldFxSize()
        Row {
            SpacerWithOldFxSize()
            DeephysText(s = message)
        }
    }
}

@Composable
fun SpacerWithOldFxSize() = Spacer(Modifier.size(oldFxSpacerSize))
val oldFxSpacerSize = 20.dp
