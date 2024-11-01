package matt.nn.deephys.gui.global.tooltip.symbol

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import matt.compose.graphics.icon.MyIcon2
import matt.compose.graphics.text.MyText
import matt.compose.graphics.tooltip.AreaWithTooltipInSupportedPlatforms
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipContent
import matt.obs.bindings.str.ObsS


const val DEEPHYS_SYMBOL_SPACING = 5.0

@Composable
fun DeephysInfoSymbol(text: ObsS) = DeephysInfoSymbol(text.value)


@Composable
fun DeephysInfoSymbol(info: String) {
    AreaWithTooltipInSupportedPlatforms(
        tooltip = {
            DeephysTooltipContent {
                MyText(info)
            }
        },
        content = {
            MyIcon2(Icons.Default.Info)
        }
    )
}


@Composable
fun DeephysTutorialSymbol(text: ObsS) = DeephysTutorialSymbol(text.value)

@Composable
fun DeephysTutorialSymbol(info: String) {
    AreaWithTooltipInSupportedPlatforms(
        tooltip = {
            DeephysTooltipContent {
                MyText(info)
            }
        },
        content = {
            MyIcon2(Icons.Default.QuestionMark)
        }
    )
}
@Composable
fun DeephysWarningSymbol(text: ObsS) = DeephysWarningSymbol(text.value)

@Composable
fun DeephysWarningSymbol(info: String) {
    AreaWithTooltipInSupportedPlatforms(
        tooltip = {
            DeephysTooltipContent {
                MyText(info)
            }
        },
        content = {
            MyIcon2(Icons.Default.Warning)
        }
    )
}


@Composable
fun DeephysSevereWarningSymbol(text: ObsS) = DeephysSevereWarningSymbol(text.value)

@Composable
fun DeephysSevereWarningSymbol(info: String) {
    AreaWithTooltipInSupportedPlatforms(
        tooltip = {
            DeephysTooltipContent {
                MyText(info)
            }
        },
        content = {
            MyIcon2(Icons.Default.Emergency)
        }
    )
}
