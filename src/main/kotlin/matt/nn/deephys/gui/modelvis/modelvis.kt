@file:Suppress("KotlinConstantConditions", "UnusedVariable", "LocalVariableName", "UNUSED_VARIABLE")

package matt.nn.deephys.gui.modelvis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import matt.compose.graphics.layout.onDpSizeChanged
import matt.compose.state.shortcuts.rememberMutableStateOf
import matt.lang.assertions.require.requireNull
import matt.lang.collect.dropLast
import matt.lang.common.unsafeError
import matt.lang.passert.powerRequire
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.unsafemigration.NeuronCircle
import matt.nn.deephys.model.importformat.Model
import matt.obs.prop.writable.BindableProperty
import matt.prim.common.exportfromlang.model.dir.Direction2D.Cardinal.Horizontal
import matt.prim.common.exportfromlang.model.dir.Direction2D.Cardinal.Vertical
import matt.prim.common.exportfromlang.model.dir.Direction2D.Cardinal.VerticalOrHorizontal
import kotlin.math.min

class ModelVisualizerState {
    @Suppress("unused")
    var dsetViewsBox: DSetViewsState? = null
        set(value) {
            requireNull(field)
            requireNotNull(value)
            powerRequire(circles!!.isNotEmpty())
            field = value
            @Suppress("SENSELESS_COMPARISON")
            if (value == null) {
                @Suppress("UNUSED_ANONYMOUS_PARAMETER")
                circles!!.forEach { circ ->
                    unsafeError(
                        """
                        circ.isHighlighted.unbind()
                        circ.isHighlighted v false    
                        """.trimIndent()
                    )
                }
            }
            @Suppress("UNUSED_ANONYMOUS_PARAMETER")
            circles!!.forEach { circ ->
                unsafeError(
                    """
                    val n = circ.neuron
                    circ.isHighlighted v (n in value.highlightedNeurons.value)
                    circ.isHighlighted.bind(
                        value.highlightedNeurons.binding {
                            n in it
                        }
                    )          
                    """.trimIndent()
                )
            }
        }

    @Suppress("VarCouldBeVal")
    var circles: List<NeuronCircle>? = null
}

@Suppress("UnusedParameter", "unused")
@Composable
fun ModelVisualizer(
    state: ModelVisualizerState,
    model: Model,
    settings: DeephysSettingsController
) {

    val ORIENTATION: VerticalOrHorizontal = Vertical
    val PREF_HEIGHT = 100.0
    val MARGIN_RATIO = 0.1
    val DIAGRAM_RATIO = 1.0 - MARGIN_RATIO * 2
    val DIAGRAM_HEIGHT = PREF_HEIGHT * DIAGRAM_RATIO
    val DIAGRAM_TOP = PREF_HEIGHT * MARGIN_RATIO
    val COLOR: Color = Color.Blue

    Box(
        Modifier
            .size(
                height = PREF_HEIGHT.dp,
                width = Double.MAX_VALUE.dp
            )
            .onDpSizeChanged {
            }
    ) {
        val diagramHeightProp = BindableProperty(DIAGRAM_HEIGHT).value.dp
        val diagramTopProp = BindableProperty(DIAGRAM_TOP).value.dp
        val widthProperty = rememberMutableStateOf<Dp?>(null)
        val diagramWidthProp = widthProperty.value?.let { it * DIAGRAM_RATIO.toFloat() }
        val diagramLeftProp = widthProperty.value?.let { it * MARGIN_RATIO.toFloat() }

        val totalSpaceForOneLayer =
            when (ORIENTATION) {
                Vertical   -> diagramWidthProp
                Horizontal -> diagramHeightProp
            }

        val totalSpaceForAllLayers =
            when (ORIENTATION) {
                Vertical   -> diagramHeightProp
                Horizontal -> diagramWidthProp
            }
        val modelStart =
            when (ORIENTATION) {
                Vertical   -> diagramTopProp
                Horizontal -> diagramLeftProp
            }
        val layerStart =
            when (ORIENTATION) {
                Vertical   -> diagramLeftProp
                Horizontal -> diagramTopProp
            }

        val spacePerLayer = totalSpaceForAllLayers!!.value / model.layers.size.toDouble()

        state.circles =
            model.resolvedLayers.flatMapIndexed { layIndex, lay ->

                val spacePerNeuron = totalSpaceForOneLayer!!.value / lay.neurons.size.toDouble()

                val radius = min(spacePerNeuron * 0.25, spacePerLayer * 0.25)

                val layerCenter = modelStart!!.value + spacePerLayer * layIndex.toDouble() + spacePerLayer / 2.0

                DeephysText(
                    s = lay.layerID,
                    modifier =
                        Modifier.offset(
                            x = (
                                when (ORIENTATION) {
                                    Vertical   -> diagramLeftProp!!.value / 4.0
                                    Horizontal -> layerCenter
                                }.dp
                            ),
                            y = (
                                when (ORIENTATION) {
                                    Vertical   -> layerCenter
                                    Horizontal -> diagramTopProp.value / 2.0
                                }.dp
                            )
                        )
                )

                lay.neurons.mapIndexed { neuronIndex, neuron ->

                    val neuronCenter = layerStart!!.value + spacePerNeuron * neuronIndex.toDouble() + spacePerNeuron / 2.0

                    val xProp =
                        when (ORIENTATION) {
                            Vertical   -> neuronCenter
                            Horizontal -> layerCenter
                        }
                    val yProp =
                        when (ORIENTATION) {
                            Vertical   -> layerCenter
                            Horizontal -> neuronCenter
                        }

                    unsafeError(
                        $$"""
                                NeuronCircle(
                        layer = lay,
                        neuron = neuron,
                        x = xProp,
                        y = yProp,
                        radius = radius,
                        color = COLOR
                    ).apply {
                        veryLazyDeephysTooltip("neuron ${'$'}neuronIndex", settings = memSafeSettings)
                        setOnMouseClicked {
                            val dvb = this@ModelVisualizer.dsetViewsBox!!
                            if (dvb.children.isEmpty()) return@setOnMouseClicked
                            if (dvb.bound.value == null) {
                                dvb.selectViewerToBind(dvb.children.first())
                            }
                            val b = dvb.bound.value!!
                            b.neuronSelection v null
                            b.layerSelection v lay.interTest
                            b.neuronSelection v neuron.interTest
                            b.view v ByNeuron
                        }
                    }
                        """.trimIndent()
                    )
                }
            }

        unsafeError(
            """
        addAll(circles!!)
        val circlesByNeuron = circles!!.associateBy { it.neuron }        
            """.trimIndent()
        )

        model.resolvedLayers.dropLast().forEachIndexed { index, layer ->
            val nextLayer = model.resolvedLayers[index + 1]
            val nextLayerNeurons = nextLayer.neurons
            layer.neurons.forEach { neuron1 ->
                unsafeError(
                    """
                val point1 = circlesByNeuron[neuron1]!!.toPoint()
                nextLayerNeurons.forEach { neuron2 ->
                    val point2 = circlesByNeuron[neuron2]!!.toPoint()
                    
                    LineWrapper().apply {
                        startXProperty.bind(point1.x)
                        startYProperty.bind(point1.y)
                        endXProperty.bind(point2.x)
                        endYProperty.bind(point2.y)
                        stroke = Color.YELLOW
                        node.strokeWidth = 0.1
                    }
                }            
                    """.trimIndent()
                )
            }
        }
    }
}
