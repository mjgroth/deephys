@file:Suppress("unused")

package matt.nn.deephys.gui.global

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import matt.compose.controls.buttons.MyButton
import matt.compose.controls.check.MyCheckbox
import matt.compose.controls.click.ActionText
import matt.compose.controls.composers.action.common.ActionButton
import matt.compose.controls.icon.MyClickableIcon
import matt.compose.controls.text.MyClickableText
import matt.compose.graphics.Compose
import matt.compose.graphics.layout.AlignedRow
import matt.compose.graphics.text.MyText
import matt.compose.state.lang.ALWAYS_TRUE
import matt.compose.state.prop.rememberBoundComposeState
import matt.compose.state.readonly.LAZY_STATE_PROBLEM_ALT
import matt.compose.state.readonly.LAZY_STATE_PROBLEM_REASON
import matt.compose.state.statefulmodel.action.SimpleAction
import matt.compose.state.toggle.NewToggleMechanism
import matt.lang.anno.Alert
import matt.lang.anno.CodeAlertCategory.TechnicalIssue
import matt.lang.anno.optin.UnsafeMattCode
import matt.lang.common.unsafeError
import matt.lang.common.unsafeReturningErr
import matt.lang.function.Op
import matt.log.warn.common.warn
import matt.math.numalg.precision.withPrecision
import matt.nn.deephys.gui.global.color.DeephysPalette
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.obs.bindings.str.ObsS
import matt.obs.prop.ObsVal
import matt.obs.prop.writable.Var
import matt.prim.converters.StringConverter

/*null because gets in the way of existing animations for pie slices
val DEEPHYS_FADE_DUR = 500.milliseconds*/
val DEEPHYS_FADE_DUR = null


@Suppress("UnusedVariable", "UNUSED_VARIABLE", "UnusedParameter")
@Composable
inline fun <reified E : Any> DeephysSpinner(
    selected: MutableState<E>,
    label: String,
    choices: List<E>,
    crossinline defaultChoice: () -> E,
    converter: StringConverter<E>,
    viewer: DatasetViewerState,
    getCurrent: State<E?>,
    crossinline acceptIf: (E) -> Boolean,
    crossinline navAction: DatasetViewerState.(E) -> Unit

) {
    @Suppress("VarCouldBeVal", "CAN_BE_VAL")
    var theValueProp: ObsVal<E>? = null
    unsafeError(
        """
        AlignedRow {
            


            var badText: ObsB? = null

            val theSpinner =
                MySpinner(
                    items = choices.toBasicObservableList(),
                    editable = true,
                    enableScroll = false,
                    converter = converter
                ) {

                    autoCommitOnType()

                    valueFactory!!.wrapAround = true

                    badText =
                        textProperty.binding {
                            it == null || !it.isInt() || it.toInt() !in choices.indices
                        }
                /*
                      border = FXBorder.solid(Color.TRANSPARENT, 10.0)
                      badText!!.onChange {
                        border = if (it) FXBorder.solid(Color.RED, 10.0)
                        else FXBorder.solid(Color.TRANSPARENT, 10.0)
                      }*/

                    val current = getCurrent.value?.takeIf { acceptIf(it) } ?: defaultChoice()
                    valueFactory!!.valueProperty.value = current

                    val rBlocker = RecursionBlocker()

                    valueFactory!!.valueProperty.onChangeWithWeak(viewer) { deRefedViewer, selection ->
                        rBlocker.with {
                            deRefedViewer.navAction(selection)
                        }
                    }



                    getCurrent.onChangeWithWeak(this) { _, newValue ->
                        rBlocker.with {
                            if (newValue != null && acceptIf(newValue)) {
                                valueFactory!!.valueProperty v newValue
                            } else {
                                valueFactory!!.valueProperty v defaultChoice()
                            }
                        }
                    }

                    theValueProp = valueFactory!!.valueProperty
                }


            if (viewer.isUnboundToDSet.value) {
                DeephysLabeledControl(label, theSpinner) {
                    backgroundProperty.bindWeakly(
                        badText!!.binding {
                            if (it) backgroundFromColor(FXColor.RED)
                            else null
                        }
                    )
                }
            }

            if (viewer.isUnboundToDSet.and(badText!!).value) {
                DeephysText("please input valid integer index between 0 and ${choices.size}")
            }
        } to theValueProp!!        
        """.trimIndent()
    )
}


@Composable
fun DeephysLabeledControl(
    label: String,
    control: Compose
) = AlignedRow {
    AlignedRow(
        Modifier.width(60.dp)
    ) {
        DeephysText("$label:")
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
        DeephysText("$label:")
    }
    Spacer(Modifier.width(5.dp))
    Box(Modifier.width(500.dp), propagateMinConstraints = true) {
        control()
    }
}


@UnsafeMattCode(saferAlternative = LAZY_STATE_PROBLEM_ALT, reason = LAZY_STATE_PROBLEM_REASON)
@Composable
fun DeephysText(
    s: ObsS
) {
    DeephysText(s.rememberBoundComposeState().value)
}

@Composable
fun DeephysText(
    s: String = "",
    font: FontFamily = DEEPHYS_FONT_DEFAULT,
    modifier: Modifier = Modifier
) {
    MyText(s, font = font, modifier = modifier)
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
        when (num) {
            is Float  -> num.withPrecision(sett).toString()
            is Double -> num.withPrecision(sett).toString()
            else      -> error("not ready for different dtype")
        } + numSuffix
    )
}



fun subtitleFont() = DEEPHY_FONT_SUBTITLE

fun titleFont() = DEEPHY_FONT_TITLE

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
fun DeephysLabel(
    s: String = "",
    font: FontFamily = DEEPHYS_FONT_DEFAULT
) {
    MyText(s, font = font)
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
    s: String = "",
    modifier: Modifier = Modifier,
    prop: Var<Boolean>? = null
) = MyCheckbox(
    label = s,
    modifier = modifier,
    checked = prop!!.rememberBoundComposeState() as MutableState
)

@Composable
fun DeephyButton(
    s: String = "",
    modifier: Modifier = Modifier,
    action: () -> Unit
) = MyButton(
    s,
    font = DEEPHYS_FONT_DEFAULT,
    modifier = modifier
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

private const val DEEPHY_ICON_BUTTON_SIZE = 25

@Suppress("UnusedParameter")
@Composable
fun DeephyIconButton(
    icon: String,
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
        "ICON HERE"
    ) {
        action()
    }
}

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

fun deephysSingleCharButtonFont() {
    unsafeError(
        """
        font.fixed().copy(size = DEEPHYS_SINGLE_CHAR_BUTTON_FONT_SIZE).fx()    
        """.trimIndent()
    )
}

const val DEEPHYS_SINGLE_CHAR_BUTTON_FONT_SIZE = 18.0

const val DEEPHYS_LATEX_TOOLTIP_SCALE = 0.70

@OptIn(ExperimentalTextApi::class)
val DEEPHYS_FONT_DEFAULT: FontFamily by lazy {
    FontFamily("Georgia")
}
val DEEPHY_FONT_SUBTITLE: FontFamily by lazy {
    unsafeReturningErr(
        """
        DEEPHYS_FONT_DEFAULT.fixed().copy(size = DEEPHYS_FONT_DEFAULT.size * 1.2).fx()    
        """.trimIndent()
    )
}
val DEEPHY_FONT_TITLE : FontFamily by lazy {
    unsafeReturningErr(
        """
        DEEPHYS_FONT_DEFAULT.fixed().copy(size = DEEPHYS_FONT_DEFAULT.size * 1.5).fx()
        """
    )
}
val DEEPHY_FONT_TITLE_BOLD: FontFamily by lazy {
    unsafeReturningErr(
        """
        DEEPHY_FONT_TITLE.fixed().copy(weight = BOLD).fx()        
        """.trimIndent()
    )
}


@Composable
fun DeephysNullMessageFact(message: String) {
    Column {
        SpacerWithOldFxSize()
        Row {
            SpacerWithOldFxSize()
            DeephysText(message)
        }
    }
}

@Composable
fun SpacerWithOldFxSize() = Spacer(Modifier.size(oldFxSpacerSize))
val oldFxSpacerSize = 20.dp
