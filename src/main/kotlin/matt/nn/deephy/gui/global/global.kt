package matt.nn.deephy.gui.global

import javafx.scene.Cursor
import javafx.scene.control.ToggleGroup
import javafx.scene.control.Tooltip
import javafx.scene.text.Font
import javafx.scene.text.FontWeight.BOLD
import javafx.util.Duration
import matt.fx.control.lang.actionbutton
import matt.fx.control.proto.actiontext.actionText
import matt.fx.control.tooltip.add
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

/*todo: a single matt.fx.control.tooltip.tooltip can be installed on multiple nodes, (and this seems important for performance)*/
fun NodeWrapper.deephyTooltip(s: String, op: Tooltip.()->Unit = {}) = DeephyTooltip(s).apply(op).also {
  add(it)
}

fun DeephyTooltip(s: String) = Tooltip(s).apply {
  font = DEEPHY_FONT_DEFAULT
  showDelay = Duration.millis(100.0)
  hideDelay = Duration.millis(1000.0)
}

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

private val DEEPHY_FONT_DEFAULT: Font = Font.font("Georgia")
private val DEEPHY_FONT_SUBTITLE = DEEPHY_FONT_DEFAULT.fixed().copy(size = DEEPHY_FONT_DEFAULT.size*1.5).fx()
private val DEEPHY_FONT_TITLE = DEEPHY_FONT_DEFAULT.fixed().copy(size = DEEPHY_FONT_DEFAULT.size*2).fx()
private val DEEPHY_FONT_TITLE_BOLD = DEEPHY_FONT_TITLE.fixed().copy(weight = BOLD).fx()

