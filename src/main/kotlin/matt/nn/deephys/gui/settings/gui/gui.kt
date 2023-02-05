package matt.nn.deephys.gui.settings.gui

import javafx.scene.control.ContentDisplay.RIGHT
import matt.async.thread.ThreadReport
import matt.fx.control.inter.contentDisplay
import matt.fx.control.inter.graphic
import matt.fx.control.lang.actionbutton
import matt.fx.control.mstage.ShowMode.DO_NOT_SHOW
import matt.fx.control.mstage.WMode.CLOSE
import matt.fx.control.win.interact.openInNewWindow
import matt.fx.control.wrapper.control.slider.slider
import matt.fx.control.wrapper.control.spinner.spinner
import matt.fx.graphics.wrapper.imageview.ImageViewWrapper
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.gui.option.BoolSetting
import matt.gui.option.DoubleSetting
import matt.gui.option.EnumSetting
import matt.gui.option.IntSetting
import matt.log.profile.mem.MemReport
import matt.nn.deephys.gui.DEEPHYS_LOG_CONTEXT
import matt.nn.deephys.gui.global.deephyActionButton
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyCheckbox
import matt.nn.deephys.gui.global.deephyRadioButton
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.deephysLabel
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.settings.DeephySettings
import matt.nn.deephys.init.gearImage
import matt.nn.deephys.state.DeephyState
import matt.prim.str.elementsToString
import java.awt.Desktop

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
	title = "Deephys Options",
  ).apply {
	width = 1000.0
  }
}

fun <E: Enum<E>> EnumSetting<E>.createRadioButtons(rec: NodeWrapper) = rec.apply {
  val tm = createBoundToggleMechanism()
  cls.java.enumConstants.forEach {
	deephyRadioButton((it as Enum<*>).name, tm, it) {
	  isSelected = prop.value == it
	}
  }
}

object SettingsPane: VBoxWrapperImpl<NodeWrapper>() {
  init {


	DeephySettings.settings.forEach { sett ->

	  when (sett) {
		is EnumSetting   -> {		//		  val group = sett.createBoundToggleMechanism()
		  hbox<NW> {
			deephyText(sett.label)

			sett.createRadioButtons(this@hbox)

			//			sett.cls.java.enumConstants.forEach {
			//			  deephyRadioButton((it as Enum<*>).name, group, it) {
			//				isSelected = sett.prop.value == it
			//			  }
			//			}
			veryLazyDeephysTooltip(sett.tooltip)
		  }

		  //		  group.selectedValue.bindBidirectional(sett.prop)

		  //		  val prop = group.selectedValue/*<Any>().toNonNullableProp()*/
		  //		  prop.value = sett.prop.value
		  //		  prop.onChange {
		  //			sett.prop::value.set(it)
		  //		  }
		}

		is IntSetting    -> {
		  deephysLabel {
			veryLazyDeephysTooltip(sett.tooltip)
			text = sett.label
			contentDisplay = RIGHT
			graphic = spinner(
			  min = sett.min, max = sett.max, initialValue = sett.prop.value, editable = true
			) {
			  prefWidth = 150.0			/* val rBlocker = RecursionBlocker()
			   valueProperty.onChange {
				 require(it != null)
				 rBlocker.with {
				   sett.prop.value = it
				 }
			   }
			   sett.prop.onChange {
				 rBlocker.with {

				 }
			   }*/
			  this.valueFactory!!.valueProperty.bindBidirectional(sett.prop)
			}
		  }
		}

		is DoubleSetting -> {
		  deephysLabel {
			veryLazyDeephysTooltip(sett.tooltip)
			text = sett.label
			contentDisplay = RIGHT
			graphic = slider(
			  min = sett.min,
			  max = sett.max,
			  value = sett.prop.value,
			) {
			  prefWidth = 150.0
			  /*  val rBlocker = RecursionBlocker()
				valueChangingProperty.onChange {
				  rBlocker.with {
					sett.prop.value = value
				  }
				}			*//*  valueProperty.onChange {
				  require(it != null)
				  rBlocker.with {
					sett.prop.value = it
				  }
				}*//*
			  sett.prop.onChange {
				rBlocker.with {
				  value = it
				}
			  }
*/
			  valueProperty.bindBidirectional(sett.prop)

			  /*this.valueFactory!!.valueProperty.bindBidirectional(sett.prop)*/
			}
		  }
		}

		is BoolSetting   -> {
		  deephyCheckbox(
			sett.label
		  ) {
			veryLazyDeephysTooltip(sett.tooltip)
			selectedProperty.bindBidirectional(sett.prop)			/*isSelected = sett.prop.value
			selectedProperty.onChange {
			  sett.prop.value = it
			}*/
		  }
		}
	  }


	}

	deephyActionButton("Reset all settings to default") {
	  DeephySettings.settings.forEach {
		it.resetToDefault()
	  }
	}

	deephyActionButton("Delete state") {
	  DeephyState.delete()
	  println("model=${DeephyState.model.value}")
	  println("tests=${DeephyState.tests.value?.elementsToString()}")
	}


	deephyButton("Print RAM info to console") {
	  setOnAction {
		println(MemReport())
	  }
	}

	deephyButton("Print thread info to console") {
	  setOnAction {
		println(ThreadReport())
	  }
	}
	deephyButton("Open Log Folder") {
	  setOnAction {
		Desktop.getDesktop().browseFileDirectory(DEEPHYS_LOG_CONTEXT.logFolder)
	  }
	}

  }
}