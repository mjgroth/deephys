package matt.nn.deephys.gui.modelvis

import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.node.line.LineWrapper
import matt.fx.graphics.wrapper.pane.PaneWrapperImpl
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.modelvis.neuroncirc.NeuronCircle
import matt.nn.deephys.model.importformat.Model
import matt.obs.bind.binding
import matt.obs.math.double.min
import matt.obs.math.double.op.div
import matt.obs.math.double.op.plus
import matt.obs.math.double.op.times
import matt.obs.prop.BindableProperty

class ModelVisualizer(model: Model): PaneWrapperImpl<Pane, NodeWrapper>(Pane()) {

  companion object {
	private val ORIENTATION = VERTICAL
	private const val PREF_HEIGHT = 100.0
	private const val MARGIN_RATIO = 0.1
	private const val DIAGRAM_RATIO = 1.0 - MARGIN_RATIO*2
	private const val DIAGRAM_HEIGHT = PREF_HEIGHT*DIAGRAM_RATIO
	private const val DIAGRAM_TOP = PREF_HEIGHT*MARGIN_RATIO
	private val COLOR: Color = Color.BLUE
  }

  var dsetViewsBox: DSetViewsVBox? = null
	set(value) {
	  require(field == null)
	  require(value != null)
	  require(circles!!.isNotEmpty())
	  field = value
	  @Suppress("SENSELESS_COMPARISON")
	  if (value == null) {
		circles!!.forEach { circ ->
		  circ.isHighlighted.unbind()
		  circ.isHighlighted v false
		}
	  }
	  circles!!.forEach { circ ->
		val n = circ.neuron
		circ.isHighlighted v (n in value.highlightedNeurons.value)
		circ.isHighlighted.bind(
		  value.highlightedNeurons.binding {
			n in it
		  }
		)
	  }
	}

  private var circles: List<NeuronCircle>? = null

  init {
	prefHeight = PREF_HEIGHT
	prefWidth = Double.MAX_VALUE
	val diagramHeightProp = BindableProperty(DIAGRAM_HEIGHT)
	val diagramTopProp = BindableProperty(DIAGRAM_TOP)
	val diagramWidthProp = widthProperty*DIAGRAM_RATIO
	val diagramLeftProp = widthProperty*MARGIN_RATIO

	val totalSpaceForOneLayer = when (ORIENTATION) {
	  VERTICAL   -> diagramWidthProp
	  HORIZONTAL -> diagramHeightProp
	}
	val totalSpaceForAllLayers = when (ORIENTATION) {
	  VERTICAL   -> diagramHeightProp
	  HORIZONTAL -> diagramWidthProp
	}

	val modelStart = when (ORIENTATION) {
	  VERTICAL   -> diagramTopProp
	  HORIZONTAL -> diagramLeftProp
	}
	val layerStart = when (ORIENTATION) {
	  VERTICAL   -> diagramLeftProp
	  HORIZONTAL -> diagramTopProp
	}


	val spacePerLayer = totalSpaceForAllLayers/model.layers.size.toDouble()

	circles = model.resolvedLayers.flatMapIndexed { layIndex, lay ->


	  val spacePerNeuron = totalSpaceForOneLayer/lay.neurons.size.toDouble()

	  val radius = min(spacePerNeuron*0.25, spacePerLayer*0.25)


	  val layerCenter = modelStart + spacePerLayer*layIndex.toDouble() + spacePerLayer/2.0

	  deephyText(lay.layerID) {
		layoutXProperty bind when (ORIENTATION) {
		  VERTICAL   -> diagramLeftProp/4.0
		  HORIZONTAL -> layerCenter
		}
		layoutYProperty bind when (ORIENTATION) {
		  VERTICAL   -> layerCenter
		  HORIZONTAL -> diagramTopProp/2.0
		}
	  }

	  lay.neurons.mapIndexed { neuronIndex, neuron ->


		val neuronCenter = layerStart + spacePerNeuron*neuronIndex.toDouble() + spacePerNeuron/2.0


		val xProp = when (ORIENTATION) {
		  VERTICAL   -> neuronCenter
		  HORIZONTAL -> layerCenter
		}
		val yProp = when (ORIENTATION) {
		  VERTICAL   -> layerCenter
		  HORIZONTAL -> neuronCenter
		}

		NeuronCircle(
		  layer = lay, neuron = neuron, x = xProp, y = yProp, radius = radius, color = COLOR
		).apply {
		  deephyTooltip("neuron $neuronIndex")
		  setOnMouseClicked {
			val dvb = this@ModelVisualizer.dsetViewsBox!!
			if (dvb.children.isEmpty()) return@setOnMouseClicked
			if (dvb.bound.value == null) {
			  dvb.myToggleGroup.selectedValue.value = dvb.children.first()
			  //			  dvb.bound.value = /
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
		  +LineWrapper().apply {
			startXProperty.bind(point1.x)
			startYProperty.bind(point1.y)
			endXProperty.bind(point2.x)
			endYProperty.bind(point2.y)
			stroke = Color.YELLOW
			node.strokeWidth = 0.1
		  }
		}
	  }
	}
  }
}


