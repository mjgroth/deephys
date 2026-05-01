package matt.nn.deephys.gui.modelvis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import matt.compose.controls.mouse.desktop.onClick
import matt.compose.graphics.color.ComposeColor
import matt.compose.state.shortcuts.rememberMutableStateOf
import matt.lang.assertions.require.requireNull
import matt.nn.deephys.gui.dataset.DatasetNodeView
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.modelvis.neuroncirc.NeuronCircle
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.model.importformat.Model
import matt.obs.prop.writable.BindableProperty
import matt.prim.common.exportfromlang.model.dir.Direction2D.Cardinal.Horizontal
import matt.prim.common.exportfromlang.model.dir.Direction2D.Cardinal.Vertical
import matt.prim.common.exportfromlang.model.dir.Direction2D.Cardinal.VerticalOrHorizontal
import kotlin.math.min

class ModelVisualizerState {

    val model = mutableStateOf<Model?>(null)

    var dsetViewsBox: DSetViewsState? = null
        set(value) {
            requireNull(field)
            requireNotNull(value)
            field = value
        }
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

        model.resolvedLayers.forEachIndexed { layIndex, lay ->

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

            lay.neurons.forEachIndexed { neuronIndex, neuron1 ->

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

                val dsetViewsBox = state.dsetViewsBox
                DeephysTooltipArea(
                    settings,
                    "neuron $neuronIndex"
                ) {
                    NeuronCircle(
                        layer = lay,
                        neuron = neuron1,
                        x = xProp,
                        y = yProp,
                        radius = radius,
                        color = COLOR,
                        isHighlighted =
                            if (dsetViewsBox == null) {
                                false
                            } else {
                                neuron1 in dsetViewsBox.highlightedNeurons.value
                            },
                        Modifier.onClick {
                            val dvb = dsetViewsBox!!
                            val dSets = dvb.datasets
                            if (dvb.bound.value == null) {
                                dvb.selectViewerToBind(dSets.first())
                            }
                            val b = dvb.bound.value!!
                            b.neuronSelection.value = null
                            b.manualLayerSelected.value = lay.interTest
                            b.neuronSelection.value = neuron1.interTest
                            b.manuallySelectedView.value = DatasetNodeView.ByNeuron
                        }
                    )
                }
                if (layIndex != model.resolvedLayers.lastIndex) {
                    val layIndex2 = layIndex + 1
                    val nextLay = model.resolvedLayers[layIndex2]
                    val layerCenter2 = modelStart.value + spacePerLayer * layIndex2.toDouble() + spacePerLayer / 2.0
                    nextLay.neurons.forEachIndexed { neuronIndex2, neuron2 ->
                        val neuronCenter2 =
                            layerStart.value +
                                spacePerNeuron * neuronIndex2.toDouble() +
                                spacePerNeuron / 2.0
                        val xProp2 =
                            when (ORIENTATION) {
                                Vertical   -> neuronCenter2
                                Horizontal -> layerCenter2
                            }
                        val yProp2 =
                            when (ORIENTATION) {
                                Vertical   -> layerCenter2
                                Horizontal -> neuronCenter2
                            }
                        Box(
                            Modifier
                                .matchParentSize()
                                .drawBehind {
                                    drawLine(
                                        start =
                                            Offset(
                                                x = xProp.toFloat(),
                                                y = yProp.toFloat()
                                            ),
                                        end =
                                            Offset(
                                                x = xProp2.toFloat(),
                                                y = yProp2.toFloat()
                                            ),
                                        strokeWidth = 0.1f,
                                        color = ComposeColor.Yellow
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}
