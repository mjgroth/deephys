package matt.nn.deephy.gui.global

import javafx.scene.control.ToggleGroup
import javafx.scene.control.Tooltip
import javafx.scene.text.Font
import javafx.util.Duration
import matt.fx.graphics.lang.actionbutton
import matt.fx.graphics.node.actionText
import matt.hurricanefx.wrapper.button.toggle.ToggleButtonWrapper
import matt.hurricanefx.wrapper.checkbox.CheckBoxWrapper
import matt.hurricanefx.wrapper.control.button.ButtonWrapper
import matt.hurricanefx.wrapper.label.LabelWrapper
import matt.hurricanefx.wrapper.link.HyperlinkWrapper
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.text.TextWrapper
import matt.obs.bindings.str.ObsS

fun NodeWrapper.deephyTooltip(s: String, op: Tooltip.()->Unit = {}) = tooltip(s) {
  font = DEEPHY_FONT
  showDelay = Duration.millis(100.0)
  hideDelay = Duration.millis(1000.0)
  op()
}

fun NodeWrapper.deephyText(s: String = "", op: TextWrapper.()->Unit = {}) = text(s) {
  font = DEEPHY_FONT
  op()
}

fun NodeWrapper.deephyText(s: ObsS, op: TextWrapper.()->Unit = {}) = text(s) {
  font = DEEPHY_FONT
  op()
}

fun NodeWrapper.deephyActionText(s: String = "", op: ()->Unit) = actionText(s) {
  op()
}.apply {
  font = DEEPHY_FONT
}

fun NodeWrapper.deephyLabel(s: String = "", op: LabelWrapper.()->Unit = {}) = label(s) {
  font = DEEPHY_FONT
  op()
}

fun NodeWrapper.deephyHyperlink(s: String = "", op: HyperlinkWrapper.()->Unit = {}) = hyperlink(s) {
  font = DEEPHY_FONT
  op()
}


fun NodeWrapper.deephyCheckbox(s: String = "", op: CheckBoxWrapper.()->Unit = {}) = checkbox(s) {
  font = DEEPHY_FONT
  op()
}

fun NodeWrapper.deephyButton(s: String = "", op: ButtonWrapper.()->Unit = {}) = button(s) {
  font = DEEPHY_FONT
  op()
}

fun <V> NodeWrapper.deephyToggleButton(
  s: String = "",
  value: V,
  group: ToggleGroup,
  op: ToggleButtonWrapper.()->Unit = {}
) = togglebutton(s, value = value, group = group) {
  font = DEEPHY_FONT
  op()
}

fun NodeWrapper.deephyActionButton(s: String = "", op: ButtonWrapper.()->Unit = {}) = actionbutton(s) {
  op()
}.apply {
  font = DEEPHY_FONT
}

private val DEEPHY_FONT: Font = Font.font("Georgia")

