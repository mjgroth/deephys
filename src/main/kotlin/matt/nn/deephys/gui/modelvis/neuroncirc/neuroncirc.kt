package matt.nn.deephys.gui.modelvis.neuroncirc

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import matt.compose.controls.interaction.rememberHoveredState
import matt.compose.controls.mouse.j.handPointerIcon
import matt.compose.graphics.color.ComposeColor
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.ResolvedNeuron

@Suppress("UnusedParameter", "unused")
@Composable
fun NeuronCircle(
    layer: ResolvedLayer,
    neuron: ResolvedNeuron,
    x: Double,
    y: Double,
    radius: Double,
    color: ComposeColor,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val (interactionSource, isHovered) = rememberHoveredState()
    Box(
        Modifier
            .background(shape = CircleShape, color = if (isHighlighted) ComposeColor.Red else if (isHovered) ComposeColor.Cyan else color)
            .offset(
                x = x.dp,
                y = y.dp
            )
            .size(radius.dp / 2)
            .hoverable(interactionSource)
            .handPointerIcon()
            .then(modifier)
    )
}
