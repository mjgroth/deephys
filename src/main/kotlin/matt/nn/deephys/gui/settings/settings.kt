package matt.nn.deephys.gui.settings

import javafx.scene.control.ContentDisplay.RIGHT
import matt.fx.control.lang.actionbutton
import matt.fx.control.mstage.ShowMode.DO_NOT_SHOW
import matt.fx.control.mstage.WMode.CLOSE
import matt.fx.control.win.interact.openInNewWindow
import matt.fx.control.wrapper.control.spinner.spinner
import matt.fx.graphics.wrapper.imageview.ImageViewWrapper
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.log.profile.MemReport
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyCheckbox
import matt.nn.deephys.gui.global.deephyLabel
import matt.nn.deephys.gui.global.deephyTooltip
import matt.nn.deephys.init.gearImage
import matt.nn.deephys.state.BoolSetting
import matt.nn.deephys.state.DeephySettings
import matt.nn.deephys.state.IntSetting


val settingsButton by lazy {
  actionbutton(graphic = ImageViewWrapper(gearImage.await()).apply {
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

object SettingsPane: VBoxWrapperImpl<NodeWrapper>() {
  init {

	DeephySettings.settings.forEach { sett ->

	  when (sett) {
		is IntSetting  -> {

		  deephyLabel {
			deephyTooltip(sett.tooltip)
			text = sett.label
			contentDisplay = RIGHT
			println("initial setting of ${sett.label} is ${sett.prop.value}")
			graphic = spinner(
			  min = sett.min,
			  max = sett.max,

			  initialValue = sett.prop.value
			) {
			  prefWidth = 55.0
			  valueProperty.onChange {
				require(it != null)
				println("saving ${sett.label} as ${it}?")
				sett.prop.value = it
			  }
			}
		  }
		}

		is BoolSetting -> {
		  deephyCheckbox(
			sett.label
		  ) {
			deephyTooltip(sett.tooltip)
			isSelected = sett.prop.value
			selectedProperty.onChange {
			  sett.prop.value = it
			}
		  }
		}
	  }


	}


	deephyButton("Print RAM info to console") {
	  println(MemReport())
	}


  }
}