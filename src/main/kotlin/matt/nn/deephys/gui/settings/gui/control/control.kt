package matt.nn.deephys.gui.settings.gui.control

import javafx.scene.control.ContentDisplay.RIGHT
import matt.fx.control.inter.contentDisplay
import matt.fx.control.inter.graphic
import matt.fx.control.wrapper.control.slider.slider
import matt.fx.control.wrapper.control.spinner.spinner
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.gui.option.ActionNotASetting
import matt.gui.option.BoolSetting
import matt.gui.option.DoubleSetting
import matt.gui.option.EnumSetting
import matt.gui.option.IntSetting
import matt.gui.option.Setting
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyCheckbox
import matt.nn.deephys.gui.global.deephysLabel
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.settings.gui.createRadioButtons


fun createControlFor(
    sett: Setting<*>,
    settings: DeephysSettingsController
) = run {
    VBoxW().apply {
        when (sett) {
            is EnumSetting       -> {
                h {
                    deephysText(sett.label)
                    sett.createRadioButtons(this@h)
                    veryLazyDeephysTooltip(sett.tooltip, settings)
                }
            }

            is IntSetting        -> {
                deephysLabel {
                    veryLazyDeephysTooltip(sett.tooltip, settings)
                    text = sett.label
                    contentDisplay = RIGHT
                    graphic =
                        spinner(
                            min = sett.min, max = sett.max, initialValue = sett.prop.value, editable = true
                        ) {
                            prefWidth = 150.0
                            valueFactory!!.valueProperty.bindBidirectional(sett.prop)
                        }
                }
            }

            is DoubleSetting     -> {
                if (sett.showControl) deephysLabel {
                    veryLazyDeephysTooltip(sett.tooltip, settings)
                    text = sett.label
                    contentDisplay = RIGHT
                    graphic =
                        slider(
                            min = sett.min,
                            max = sett.max,
                            value = sett.prop.value
                        ) {
                            prefWidth = 150.0
                            valueProperty.bindBidirectional(sett.prop)
                        }
                }
            }

            is BoolSetting       -> {
                deephyCheckbox(
                    sett.label
                ) {
                    veryLazyDeephysTooltip(sett.tooltip, settings)
                    selectedProperty.bindBidirectional(sett.prop)
                }
            }

            is ActionNotASetting -> {
                deephyButton(sett.label) {
                    veryLazyDeephysTooltip(sett.tooltip, settings)
                    setOnAction {
                        sett.op()
                    }
                }
            }
        }
    }.children.first()
}
