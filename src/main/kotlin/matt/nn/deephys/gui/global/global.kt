package matt.nn.deephys.gui.global

import javafx.scene.Cursor
import javafx.scene.control.ToggleGroup
import javafx.scene.text.Font
import javafx.scene.text.FontWeight.BOLD
import matt.fx.control.lang.actionbutton
import matt.fx.control.proto.actiontext.actionText
import matt.fx.control.wrapper.button.radio.RadioButtonWrapper
import matt.fx.control.wrapper.button.radio.radiobutton
import matt.fx.control.wrapper.button.toggle.ToggleButtonWrapper
import matt.fx.control.wrapper.button.toggle.togglebutton
import matt.fx.control.wrapper.checkbox.CheckBoxWrapper
import matt.fx.control.wrapper.checkbox.checkbox
import matt.fx.control.wrapper.control.button.ButtonWrapper
import matt.fx.control.wrapper.control.button.button
import matt.fx.control.wrapper.label.LabelWrapper
import matt.fx.control.wrapper.label.label
import matt.fx.control.wrapper.link.HyperlinkWrapper
import matt.fx.control.wrapper.link.hyperlink
import matt.fx.graphics.font.fixed
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.obs.bindings.str.ObsS
import matt.obs.prop.BindableProperty


fun NodeWrapper.deephyText(s: String = "", op: DeephyText.()->Unit = {}) =
  DeephyText(BindableProperty(s)).apply(op).also { +it }

fun NodeWrapper.deephyText(s: ObsS, op: DeephyText.()->Unit = {}) = DeephyText(s).apply(op).also { +it }
fun DeephyText(s: String) = DeephyText(BindableProperty(s))
class DeephyText(s: ObsS): TextWrapper() {
  init {
	textProperty.bind(s)
	font = DEEPHY_FONT_DEFAULT
  }
}

fun TextWrapper.subtitleFont() {
  font = DEEPHY_FONT_SUBTITLE
}

fun TextWrapper.titleFont() {
  font = DEEPHY_FONT_TITLE
}

fun TextWrapper.titleBoldFont() {
  font = DEEPHY_FONT_TITLE_BOLD
}

fun NodeWrapper.deephyActionText(s: String = "", op: ()->Unit) = actionText(s) {
  op()
}.apply {
  font = DEEPHY_FONT_DEFAULT
  cursor = Cursor.HAND
}

fun NodeWrapper.deephyLabel(s: String = "", op: LabelWrapper.()->Unit = {}) = label(s) {
  font = DEEPHY_FONT_DEFAULT
  op()
}

fun NodeWrapper.deephyHyperlink(s: String = "", op: HyperlinkWrapper.()->Unit = {}) = hyperlink(s) {
  font = DEEPHY_FONT_DEFAULT
  op()
}


fun NodeWrapper.deephyCheckbox(s: String = "", op: CheckBoxWrapper.()->Unit = {}) = checkbox(s) {
  font = DEEPHY_FONT_DEFAULT
  op()
}

fun NodeWrapper.deephyButton(s: String = "", theOp: ButtonWrapper.()->Unit = {}) = button(s) {
  font = DEEPHY_FONT_DEFAULT
  theOp()
}

fun <V> NodeWrapper.deephyRadioButton(
  s: String,
  group: ToggleGroup,
  value: V,
  theOp: RadioButtonWrapper.()->Unit = {}
) = radiobutton<V>(s, group, value) {
  font = DEEPHY_FONT_DEFAULT
  theOp()
}

fun <V> NodeWrapper.deephyToggleButton(
  s: String = "",
  value: V,
  group: ToggleGroup,
  op: ToggleButtonWrapper.()->Unit = {}
) = togglebutton(s, value = value, group = group) {
  font = DEEPHY_FONT_DEFAULT
  op()
}

fun NodeWrapper.deephyActionButton(s: String = "", theOp: ButtonWrapper.()->Unit = {}) = actionbutton(s) {
  theOp()
}.apply {
  font = DEEPHY_FONT_DEFAULT
}

val DEEPHY_FONT_DEFAULT: Font by lazy { Font.font("Georgia") }
val DEEPHY_FONT_SUBTITLE by lazy { DEEPHY_FONT_DEFAULT.fixed().copy(size = DEEPHY_FONT_DEFAULT.size*1.5).fx() }
val DEEPHY_FONT_TITLE by lazy { DEEPHY_FONT_DEFAULT.fixed().copy(size = DEEPHY_FONT_DEFAULT.size*2).fx() }
val DEEPHY_FONT_TITLE_BOLD by lazy { DEEPHY_FONT_TITLE.fixed().copy(weight = BOLD).fx() }

