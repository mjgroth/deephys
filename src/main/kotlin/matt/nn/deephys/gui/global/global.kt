package matt.nn.deephys.gui.global

import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.layout.Border
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight.BOLD
import matt.fx.control.lang.actionbutton
import matt.fx.control.toggle.mech.ToggleMechanism
import matt.fx.control.wrapper.button.radio.RadioButtonWrapper
import matt.fx.control.wrapper.button.radio.radiobutton
import matt.fx.control.wrapper.button.toggle.ToggleButtonWrapper
import matt.fx.control.wrapper.button.toggle.togglebutton
import matt.fx.control.wrapper.checkbox.CheckBoxWrapper
import matt.fx.control.wrapper.checkbox.checkbox
import matt.fx.control.wrapper.control.ControlWrapper
import matt.fx.control.wrapper.control.button.ButtonWrapper
import matt.fx.control.wrapper.control.button.base.ButtonBaseWrapper
import matt.fx.control.wrapper.control.button.button
import matt.fx.control.wrapper.label.LabelWrapper
import matt.fx.control.wrapper.label.label
import matt.fx.control.wrapper.link.HyperlinkWrapper
import matt.fx.control.wrapper.link.hyperlink
import matt.fx.graphics.font.fixed
import matt.fx.graphics.wrapper.ET
import matt.fx.graphics.wrapper.EventTargetWrapper
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.HBoxWrapper
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.fx.graphics.wrapper.text.textlike.MONO_FONT
import matt.fx.graphics.wrapper.text.textlike.TextLike
import matt.gui.actiontext.actionText
import matt.obs.bindings.str.ObsS
import matt.obs.prop.BindableProperty
import matt.obs.prop.Var

/*null because gets in the way of existing animations for pie slices*/
/*val DEEPHYS_FADE_DUR = 500.milliseconds*/
val DEEPHYS_FADE_DUR = null

fun ET.deephysLabeledControl(
  label: String,
  control: ControlWrapper,
  op: HBoxWrapper<NW>.()->Unit = {}
) = h {
  alignment = Pos.CENTER_LEFT
  h {
	alignment = Pos.CENTER_LEFT
	deephyText("$label:")
	prefWidth = 60.0
  }
  +control.apply {
	prefWidth = 100.0
  }
  op()
}

fun EventTargetWrapper.deephyText(s: String = "", op: DeephyText.()->Unit = {}) =
  DeephyText(BindableProperty(s)).apply(op).also { +it }

fun EventTargetWrapper.deephyText(s: ObsS, op: DeephyText.()->Unit = {}) = DeephyText(s).apply(op).also { +it }
fun DeephyText(s: String) = DeephyText(BindableProperty(s))
class DeephyText(s: ObsS): TextWrapper() {
  init {
	textProperty.bind(s)
	font = DEEPHYS_FONT_DEFAULT
  }
}

fun TextLike.subtitleFont() {
  font = DEEPHY_FONT_SUBTITLE
}

fun TextLike.titleFont() {
  font = DEEPHY_FONT_TITLE
}

fun TextLike.titleBoldFont() {
  font = DEEPHY_FONT_TITLE_BOLD
}

fun EventTargetWrapper.deephyActionText(s: String = "", op: ()->Unit) = actionText(s) {
  op()
}.apply {
  font = DEEPHYS_FONT_DEFAULT
  cursor = Cursor.HAND
}

fun EventTargetWrapper.deephysLabel(s: String = "", op: LabelWrapper.()->Unit = {}) = label(s) {
  font = DEEPHYS_FONT_DEFAULT
  op()
}

fun EventTargetWrapper.deephyHyperlink(s: String = "", op: HyperlinkWrapper.()->Unit = {}) = hyperlink(s) {
  font = DEEPHYS_FONT_DEFAULT
  op()
}


fun EventTargetWrapper.deephyCheckbox(
  s: String = "",
  prop: Var<Boolean>? = null,
  weakBothWays: Boolean? = null,
  op: CheckBoxWrapper.()->Unit = {}
) = checkbox(s, property = prop, weakBothWays = weakBothWays) {
  font = DEEPHYS_FONT_DEFAULT
  op()
}

fun EventTargetWrapper.deephyButton(s: String = "", theOp: ButtonWrapper.()->Unit = {}) = button(s) {
  deephysButtonStyle()
  theOp()
}

fun ButtonBaseWrapper<*>.deephysButtonStyle() {
  font = DEEPHYS_FONT_DEFAULT
  /*so when I highlight the button later, the layout does not change. Also the bit of space is nice.*/
  border = Border.stroke(Color.TRANSPARENT)
}

fun <V: Any> EventTargetWrapper.deephyRadioButton(
  s: String,
  group: ToggleMechanism<V>,
  value: V,
  theOp: RadioButtonWrapper.()->Unit = {}
) = radiobutton<V>(s, group, value) {
  deephysButtonStyle()
  theOp()
}

fun <V: Any> NodeWrapper.deephyToggleButton(
  s: String = "",
  value: V,
  group: ToggleMechanism<V>,
  op: ToggleButtonWrapper.()->Unit = {}
) = togglebutton(s, value = value, group = group) {
  deephysButtonStyle()
  op()
}

fun NodeWrapper.deephyActionButton(s: String = "", theOp: ButtonWrapper.()->Unit = {}) = actionbutton(s) {
  theOp()
}.apply {
  deephysButtonStyle()
}

fun ButtonWrapper.deephysSingleCharButtonFont() {
  font = font.fixed().copy(size = DEEPHYS_SINGLE_CHAR_BUTTON_FONT_SIZE).fx()
}

val DEEPHYS_SINGLE_CHAR_BUTTON_FONT_SIZE = 18.0

val DEEPHYS_FONT_DEFAULT: Font by lazy { Font.font("Georgia") }
val DEEPHYS_FONT_MONO by lazy {
  DEEPHYS_FONT_DEFAULT.fixed().copy(family = MONO_FONT.family).fx()
}
val DEEPHY_FONT_SUBTITLE by lazy { DEEPHYS_FONT_DEFAULT.fixed().copy(size = DEEPHYS_FONT_DEFAULT.size*1.5).fx() }
val DEEPHY_FONT_TITLE by lazy { DEEPHYS_FONT_DEFAULT.fixed().copy(size = DEEPHYS_FONT_DEFAULT.size*2).fx() }
val DEEPHY_FONT_TITLE_BOLD by lazy { DEEPHY_FONT_TITLE.fixed().copy(weight = BOLD).fx() }


val deephysNullMessageFact: (message: String)->NW = { message ->
  VBoxW().apply {
	spacer()
	h {
	  spacer()
	  deephyText(message)
	}
  }
}