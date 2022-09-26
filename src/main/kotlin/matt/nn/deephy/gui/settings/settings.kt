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
import matt.nn.deephy.state.BoolSetting
import matt.nn.deephy.state.DeephySettings
import matt.nn.deephy.state.IntSetting


val settingsButton by lazy {
  actionbutton(graphic = ImageViewWrapper(Image(resourceStream("gear.png"))).apply {
	isPreserveRatio = true
	fitWidth = 25.0
  }) {
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

	DeephySettings.settings.forEach { sett ->

	  when (sett) {
		is IntSetting  -> {

		  label {
			tooltip(sett.tooltip)
			font = DEEPHY_FONT
			text = sett.label
			contentDisplay = RIGHT
			graphic = spinner(
			  min = sett.min,
			  max = sett.max,
			  initialValue = sett.prop.value
			) {
			  prefWidth = 55.0
			  valueProperty().onChange {
				require(it != null)
				sett.prop.value = it
			  }
			}
		  }
		}

		is BoolSetting -> {
		  checkbox(
			sett.label
		  ) {
			tooltip(sett.tooltip)
			font = DEEPHY_FONT
			isSelected = sett.prop.value
			selectedProperty.onChange {
			  sett.prop.value = it
			}
		  }
		}
	  }


	}


  }
}