package matt.nn.deephys.gui.modelvis

import androidx.compose.runtime.Composable
import matt.lang.assertions.require.requireNotEmpty
import matt.lang.assertions.require.requireNull
import matt.lang.common.unsafeErr
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
import matt.nn.deephys.gui.modelvis.neuroncirc.NeuronCircle
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.unsafemigration.NeuronCircle
import matt.nn.deephys.model.importformat.Model
import matt.obs.math.double.op.div
import matt.obs.math.double.op.plus
import matt.obs.math.double.op.times

class ModelVisualizerState {
    var dsetViewsBox: DSetViewsState? = null
        set(value) {
            requireNull(field)
            requireNotNull(value)
            requireNotEmpty(circles!!)
            field = value
            @Suppress("SENSELESS_COMPARISON")
            if (value == null) {
                circles!!.forEach { circ ->
                    unsafeErr(
                        """
                        circ.isHighlighted.unbind()
                        circ.isHighlighted v false    
                        """.trimIndent()
                    )
                }
            }
            circles!!.forEach { circ ->
                unsafeErr(
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
    private var circles: List<NeuronCircle>? = null
}

@Composable
fun ModelVisualizer(
    state: ModelVisualizerState,
    model: Model,
    settings: DeephysSettingsController
) {

    unsafeErr(
        """
            
        val ORIENTATION = VerticalOrHorizontal.Vertical
        val PREF_HEIGHT = 100.0
        val MARGIN_RATIO = 0.1
        val DIAGRAM_RATIO = 1.0 - MARGIN_RATIO * 2
        val DIAGRAM_HEIGHT = PREF_HEIGHT * DIAGRAM_RATIO
        val DIAGRAM_TOP = PREF_HEIGHT * MARGIN_RATIO
        val COLOR: Color = Color.Blue

        Box(
            Modifier
                .size(
                    height = PREF_HEIGHT,
                    width = Double.MAX_VALUE
                )
        ) {




            init {
                val diagramHeightProp = BindableProperty(DIAGRAM_HEIGHT)
                val diagramTopProp = BindableProperty(DIAGRAM_TOP)
                val diagramWidthProp = widthProperty * DIAGRAM_RATIO
                val diagramLeftProp = widthProperty * MARGIN_RATIO

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


                val spacePerLayer = totalSpaceForAllLayers / model.layers.size.toDouble()

                circles =
                    model.resolvedLayers.flatMapIndexed { layIndex, lay ->


                        val spacePerNeuron = totalSpaceForOneLayer / lay.neurons.size.toDouble()

                        val radius = min(spacePerNeuron * 0.25, spacePerLayer * 0.25)


                        val layerCenter = modelStart + spacePerLayer * layIndex.toDouble() + spacePerLayer / 2.0

                        DeephysText(
                            lay.layerID,
                            modifier =
                                Modifier.offset(
                                    x = (
                                        when (ORIENTATION) {
                                            Vertical   -> diagramLeftProp / 4.0
                                            Horizontal -> layerCenter
                                        }.value.dp
                                    ),
                                    y = (
                                        when (ORIENTATION) {
                                            Vertical   -> layerCenter
                                            Horizontal -> diagramTopProp / 2.0
                                        }.value.dp
                                    )
                                )
                        )

                        lay.neurons.mapIndexed { neuronIndex, neuron ->


                            val neuronCenter = layerStart + spacePerNeuron * neuronIndex.toDouble() + spacePerNeuron / 2.0


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
                        }
                    }

                addAll(circles!!)
                val circlesByNeuron = circles!!.associateBy { it.neuron }
                model.resolvedLayers.dropLast(1).forEachIndexed { index, layer ->
                    val nextLayer = model.resolvedLayers[index + 1]
                    val nextLayerNeurons = nextLayer.neurons
                    layer.neurons.forEach { neuron1 ->
                        val point1 = circlesByNeuron[neuron1]!!.toPoint()
                        nextLayerNeurons.forEach { neuron2 ->
                            val point2 = circlesByNeuron[neuron2]!!.toPoint()
                            unsafeErr(
                                ""${'"'}
                                          LineWrapper().apply {
                                    startXProperty.bind(point1.x)
                                    startYProperty.bind(point1.y)
                                    endXProperty.bind(point2.x)
                                    endYProperty.bind(point2.y)
                                    stroke = Color.YELLOW
                                    node.strokeWidth = 0.1
                                }
                                ""${'"'}.trimIndent()
                            )
                        }
                    }
                }
            }
        }
        """.trimIndent()
    )
}
