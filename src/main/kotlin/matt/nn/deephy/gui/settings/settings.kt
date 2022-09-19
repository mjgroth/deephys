package matt.nn.deephy.gui.settings

import javafx.scene.control.ContentDisplay.RIGHT
import javafx.scene.image.Image
import matt.fx.graphics.lang.actionbutton
import matt.fx.graphics.win.interact.openInNewWindow
import matt.fx.graphics.win.stage.ShowMode.DO_NOT_SHOW
import matt.fx.graphics.win.stage.WMode.CLOSE
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.tornadofx.item.spinner
import matt.hurricanefx.wrapper.imageview.ImageViewWrapper
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.lang.resourceStream
import matt.nn.deephy.gui.DEEPHY_FONT
import matt.nn.deephy.state.DeephyState



val settingsButton by lazy {
  actionbutton(graphic = ImageViewWrapper(Image(resourceStream("gear.png"))).apply {
	isPreserveRatio = true
	fitWidth = 25.0
  }.node) {
	settingsWindow.show()
  }
}

val settingsWindow by lazy {
  SettingsPane.openInNewWindow(
	DO_NOT_SHOW,
	CLOSE,
	EscClosable = true,
	decorated = true,
	title = "Deephy Options"
  )
}

object SettingsPane: VBoxWrapper<NodeWrapper>() {
  init {
	label {
	  font = DEEPHY_FONT
	  text = "Number of images per neuron in image view"
	  contentDisplay = RIGHT
	  graphic = spinner(
		min = 9,
		max = 18,
		initialValue = DeephyState.numImagesPerNeuronInByImage.value
	  ) {
		prefWidth = 55.0
		valueProperty().onChange {
		  require(it != null)
		  DeephyState.numImagesPerNeuronInByImage.value = it
		}
	  }
	}
	checkbox(
	  "Normalize activations of top neurons",
	) {
	  font = DEEPHY_FONT
	  isSelected = DeephyState.normalizeTopNeuronActivations.value!!
	  //	  prefWidth = 55.0
	  selectedProperty.onChange {
		DeephyState.normalizeTopNeuronActivations.value = it
	  }
	}
  }
}