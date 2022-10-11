package matt.nn.deephys.gui.settings

import javafx.scene.control.ContentDisplay.RIGHT
import javafx.scene.control.ToggleGroup
import matt.fx.control.lang.actionbutton
import matt.fx.control.mstage.ShowMode.DO_NOT_SHOW
import matt.fx.control.mstage.WMode.CLOSE
import matt.fx.control.tfx.control.selectedValueProperty
import matt.fx.control.win.interact.openInNewWindow
import matt.fx.control.wrapper.control.spinner.spinner
import matt.fx.graphics.wrapper.imageview.ImageViewWrapper
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.hurricanefx.eye.wrapper.obs.obsval.prop.toNonNullableProp
import matt.log.profile.mem.MemReport
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyCheckbox
import matt.nn.deephys.gui.global.deephyLabel
import matt.nn.deephys.gui.global.deephyRadioButton
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.init.gearImage
import matt.nn.deephys.state.BoolSetting
import matt.nn.deephys.state.DeephySettings
import matt.nn.deephys.state.EnumSetting
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
		is EnumSetting -> {
		  val group = ToggleGroup()
		  hbox<NW> {
			deephyText(sett.label)
			sett.cls.java.enumConstants.forEach {
			  deephyRadioButton((it as Enum<*>).name, group, it) {

				  isSelected = sett.prop.value == it
			  }
			}
			deephyTooltip(sett.tooltip)
		  }
		  val prop = group.selectedValueProperty<Any>().toNonNullableProp()
		  prop.value = sett.prop.value
		  prop.onChange {
			sett.prop::value.set(it)
		  }
		}

		is IntSetting  -> {

		  deephyLabel {
			deephyTooltip(sett.tooltip)
			text = sett.label
			contentDisplay = RIGHT
			graphic = spinner(
			  min = sett.min,
			  max = sett.max,
			  initialValue = sett.prop.value
			) {
			  prefWidth = 55.0
			  valueProperty.onChange {
				require(it != null)
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
	  setOnAction {
		println(MemReport())
	  }
	}


  }
}