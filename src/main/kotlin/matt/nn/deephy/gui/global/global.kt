package matt.nn.deephy.gui.global

import javafx.scene.control.Tooltip
import javafx.scene.text.Font
import javafx.util.Duration
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.text.TextWrapper

fun NodeWrapper.deephyTooltip(s: String, op: Tooltip.()->Unit = {}) = tooltip(s) {
  showDelay = Duration.millis(100.0)
  hideDelay = Duration.millis(1000.0)
  op()
}

fun NodeWrapper.deephyText(s: String = "", op: TextWrapper.()->Unit = {}) = text(s) {
  font = DEEPHY_FONT
  op()
}

val DEEPHY_FONT: Font = Font.font("Georgia")