@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED", "unused")

package matt.nn.deephys.gui.settings.gui.control

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import matt.compose.graphics.layout.AlignedRow
import matt.compose.state.option.BoolSetting
import matt.compose.state.option.DoubleSetting
import matt.compose.state.option.IntSetting
import matt.compose.state.option.Setting
import matt.lang.common.unsafeReturningErr
import matt.lang.context.AutomationContext
import matt.nn.deephys.gui.global.DeephyCheckbox
import matt.nn.deephys.gui.global.DeephysLabel
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.prim.pfloat.verifyWholeToInt

context(AutomationContext)
@Composable
fun CreateControlFor(
    sett: Setting<*>,
    settings: DeephysSettingsController
) {
    Column {
        DeephysTooltipArea(settings, sett.tooltip, enableTooltip = (sett as? DoubleSetting)?.showControl != false) {
            when (sett) {

                is IntSetting    -> {
                    AlignedRow {
                        DeephysLabel(
                            sett.label
                        )
                        Slider(
                            valueRange = sett.min.toFloat()..sett.max.toFloat(),
                            value = sett.prop.value.toFloat(),
                            onValueChange = {
                                sett.prop.value = it.verifyWholeToInt()
                            },
                            modifier = Modifier.width(150.dp)
                        )
                    }
                }

                is DoubleSetting -> {
                    if (sett.showControl) {
                        AlignedRow {
                            DeephysLabel(
                                sett.label
                            )
                            Slider(
                                valueRange = sett.min.toFloat()..sett.max.toFloat(),
                                value = sett.prop.value.toFloat(),
                                onValueChange = {
                                    sett.prop.value = it.toDouble()
                                },
                                modifier = Modifier.width(150.dp)
                            )
                        }
                    }
                }

                is BoolSetting   -> {
                    DeephyCheckbox(
                        sett.label,
                        unsafeReturningErr {
                            sett.prop
                        }

                    )
                }
            }
        }
    }
}
