@file:Suppress("unused")

package matt.nn.deephys.gui.modelvis.neuroncirc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import matt.compose.controls.mouse.attachHoverState
import matt.compose.graphics.color.ComposeColor
import matt.compose.state.shortcuts.rememberMutableStateOf
import matt.lang.common.D
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.ResolvedNeuron
import matt.obs.prop.ObsVal

@Suppress("UnusedParameter")
@Composable
fun NeuronCircle(
    layer: ResolvedLayer,
    neuron: ResolvedNeuron,
    x: ObsVal<D>,
    y: ObsVal<D>,
    radius: ObsVal<D>,
    color: ComposeColor,
    isHighlighted: Boolean
) {
    val hovered = rememberMutableStateOf(false)
    Box(
        Modifier
            .background(shape = CircleShape, color = if (isHighlighted) ComposeColor.Red else if (hovered.value) ComposeColor.Cyan else color)
            .offset(
                x = x.value.dp,
                y = y.value.dp
            )
            .size(radius.value.dp / 2)
            .attachHoverState(hovered)
            .pointerHoverIcon(PointerIcon.Hand)
    )
}
