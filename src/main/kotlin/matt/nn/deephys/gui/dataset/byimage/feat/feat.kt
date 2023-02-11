package matt.nn.deephys.gui.dataset.byimage.feat

import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.PaneWrapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.subtitleFont
import matt.prim.str.truncateWithElipsesOrAddSpaces

class FeaturesView(
  features: Map<String, String>
): VBoxW() {
  init {
	deephysText("Features:") {
	  subtitleFont()
	}
	spacer()
	val featureKeysBox: NodeWrapper = vbox<TextWrapper> {}
	val featureValuesBox: NodeWrapper = vbox<TextWrapper> {}
	hbox<PaneWrapper<*>> {
	  +featureKeysBox
	  spacer()
	  +featureValuesBox
	}
	features.forEach { (k, v) ->
	  featureKeysBox.deephysText(k.truncateWithElipsesOrAddSpaces(25))
	  featureValuesBox.deephysText(v)
	}
  }
}