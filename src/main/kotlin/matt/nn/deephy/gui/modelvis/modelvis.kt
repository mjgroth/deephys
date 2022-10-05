package matt.nn.deephy.gui.modelvis

import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import matt.fx.graphics.wrapper.node.line.LineWrapper
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.PaneWrapperImpl
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephy.gui.global.deephyText
import matt.nn.deephy.gui.global.deephyTooltip
import matt.nn.deephy.gui.modelvis.neuroncirc.NeuronCircle
import matt.nn.deephy.model.importformat.Model
import matt.obs.bind.MyBinding
import matt.obs.col.change.AdditionBase
import matt.obs.math.double.min
import matt.obs.math.double.op.div
import matt.obs.math.double.op.plus
import matt.obs.math.double.op.times
import matt.obs.prop.BindableProperty

class ModelVisualizer(val model: Model): PaneWrapperImpl<Pane, NodeWrapper>(Pane()) {

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
	  circles!!.forEach { circ ->
		val n = circ.neuron
		circ.isHighlighted.bind(MyBinding(value.children) {
		  value.children.any { n.interTest in it.highlightedNeurons.value }
		}.apply {
		  value.children.forEach {
			addDependency(it.highlightedNeurons)
		  }
		  value.children.onChange {
			if (it is AdditionBase) {
			  it.addedElements.forEach {
				addDependency(it.highlightedNeurons)
			  }
			}
		  }
		})
	  }
	}

  private var circles: List<NeuronCircle>? = null

  init {
	prefHeight = PREF_HEIGHT
	val diagramHeightProp = BindableProperty<Double>(DIAGRAM_HEIGHT)
	val diagramTopProp = BindableProperty<Double>(DIAGRAM_TOP)
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


	val spacePerLayer = model.layers.size.toDouble()/totalSpaceForAllLayers

	circles = model.resolvedLayers.flatMapIndexed { layIndex, lay ->

	  val spacePerNeuron = lay.neurons.size.toDouble()/totalSpaceForOneLayer
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
			  dvb.bound.value = dvb.children.first()
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


