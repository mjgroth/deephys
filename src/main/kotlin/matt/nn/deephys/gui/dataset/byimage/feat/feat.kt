package matt.nn.deephys.gui.dataset.byimage.feat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.SpacerWithOldFxSize
import matt.nn.deephys.gui.global.subtitleFont
import matt.prim.str.truncateWithEllipsesOrAddSpaces

@Composable
fun FeaturesView(
    features: Map<String, String>
) {
    Column {
        DeephysText(s = "Features:", style = subtitleFont())
        SpacerWithOldFxSize()
        Row {
            val entries = features.entries
            Column {
                entries.forEach {
                    DeephysText(s = it.key.truncateWithEllipsesOrAddSpaces(25))
                }
            }
            SpacerWithOldFxSize()
            Column {
                entries.forEach { DeephysText(s = it.value) }
            }
        }
    }
}
