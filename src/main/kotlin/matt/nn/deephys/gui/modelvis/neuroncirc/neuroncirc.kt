package matt.nn.deephys.gui.modelvis.neuroncirc

import javafx.scene.Cursor
import javafx.scene.paint.Color
import matt.fx.graphics.wrapper.node.onHover
import matt.fx.graphics.wrapper.node.shape.circle.CircleWrapper
import matt.lang.common.D
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.ResolvedNeuron
import matt.obs.bind.binding
import matt.obs.prop.ObsVal
import matt.obs.prop.writable.BindableProperty

class NeuronCircle(
    val layer: ResolvedLayer,
    val neuron: ResolvedNeuron,
    x: ObsVal<D>,
    y: ObsVal<D>,
    radius: ObsVal<D>,
    color: Color
): CircleWrapper() {

    val isHighlighted = BindableProperty(false)

    init {
        fill = color
        centerXProperty.bind(x)
        centerYProperty.bind(y)
        radiusProperty.bind(radius)
        node.viewOrder = -1.0
        cursor = Cursor.HAND
        onHover {
            fill =
                if (it) Color.AQUA
                else color
        }
        fillProperty.bind(
            isHighlighted.binding(hoverProperty) {
                when {
                    hoverProperty.value -> Color.AQUA
                    isHighlighted.value -> Color.RED
                    else                -> color
                }
            }
        )
    }
}
