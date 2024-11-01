package matt.nn.deephys.gui.dataset.byimage.feat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.SpacerWithOldFxSize
import matt.nn.deephys.gui.global.subtitleFont
import matt.prim.str.truncateWithElipsesOrAddSpaces

@Composable
fun FeaturesView(
    features: Map<String, String>
) {
    Column {
        DeephysText("Features:", font = subtitleFont())
        SpacerWithOldFxSize()
        Row {
            Column {
                features.forEach { (k, v) ->
                    DeephysText(k.truncateWithElipsesOrAddSpaces(25))
                }
            }
            SpacerWithOldFxSize()
            Column {
                features.forEach { (k, v) ->
                    DeephysText(v)
                }
            }
        }
    }
}
