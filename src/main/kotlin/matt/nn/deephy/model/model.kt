package matt.nn.deephy.model

import kotlinx.serialization.Serializable


sealed interface NeuronRef {
  val neuron: Neuron
}


@Serializable class Neuron: NeuronRef {
  override val neuron get() = this
}


class NeuronTestResults(
  override val neuron: Neuron,
  private val activations: List<Double>
): NeuronRef {

  val activationIndexesHighToLow by lazy {
	activations.withIndex().sortedBy { it.value }.reversed().map { it.index }
  }
}

interface ResolvedNeuronLike: NeuronRef {
  val index: Int
  val layer: ResolvedLayer
}

class ResolvedNeuron(
  override val neuron: Neuron,
  override val index: Int,
  override val layer: ResolvedLayer,
): ResolvedNeuronLike


class NeuronWithActivation(
  private val rNeuron: ResolvedNeuron,
  val activation: Double,
  val normalizedActivation: Double
): ResolvedNeuronLike by rNeuron

interface DeephyImageData {
  val data: List<List<List<Double>>>
  val matrix: List<List<List<Double>>>
  val activations: ModelState
}

@Serializable
class ModelState(
  val activations: List<List<Double>>
)

@Serializable class DeephyImage(
  val imageID: Int,
  val categoryID: Int,
  val category: String,
  override val data: List<List<List<Double>>>,
  override val activations: ModelState
): DeephyImageData {

  override val matrix by lazy {
	val newRows = mutableListOf<MutableList<MutableList<Double>>>()
	data.mapIndexed { colorIndex, singleColorMatrix ->
	  singleColorMatrix.mapIndexed { rowIndex, row ->
		val newRow =
		  if (colorIndex == 0) mutableListOf<MutableList<Double>>().also { newRows += it } else newRows[rowIndex]

		row.mapIndexed { colIndex, pixel ->
		  val newCol = if (colorIndex == 0) mutableListOf<Double>().also { newRow += it } else newRow[colIndex]
		  newCol += pixel
		}
	  }
	}
	newRows
  }
}

class ResolvedDeephyImage(
  image: DeephyImage,
  val index: Int,
  val test: Test,
  val model: Model
): DeephyImageData {
  val imageID = image.imageID
  val categoryID = image.categoryID
  val category = image.category
  override val data = image.data
  override val matrix by lazy { image.matrix }
  override val activations by lazy { image.activations }

  fun activationsFor(rLayer: ResolvedLayer) = activations.activations[rLayer.index]

  fun topNeurons(rLayer: ResolvedLayer): List<NeuronWithActivation> =
	activationsFor(rLayer).run {
	  val mx = max()
	  mapIndexed { neuronIndex, a ->
		NeuronWithActivation(rLayer.neurons[neuronIndex], a, normalizedActivation = a/mx)
	  }
		.sortedBy { it.activation }
		.reversed()
		.take(25)
	}

}

interface LayerLike {
  val layerID: String
  val neurons: List<NeuronRef>
  override fun toString(): String
  val isClassification get() = layerID == "classification"
}

@Serializable class Layer(
  override val layerID: String,
  override val neurons: List<Neuron>
): LayerLike {
  override fun toString() = layerID
}

class ResolvedLayer(
  layer: Layer,
  val model: Model,
  val index: Int
): LayerLike by layer {
  override val neurons: List<ResolvedNeuron> = layer.neurons.mapIndexed { index, neuron ->
	ResolvedNeuron(
	  neuron = neuron, index = index, layer = this@ResolvedLayer
	)
  }
}


interface DeephyObject {
  val name: String
  val suffix: String?
}

@Serializable class Model(
  override val name: String,
  override val suffix: String?,
  val layers: List<Layer>,
): DeephyObject {
  val resolvedLayers by lazy { layers.mapIndexed { index, layer -> ResolvedLayer(layer, this@Model, index) } }
  val neurons: List<ResolvedNeuron> by lazy { resolvedLayers.flatMap { it.neurons } }
  val classificationLayer by lazy {
	resolvedLayers.firstOrNull { it.isClassification }
  }

}

/*../../../../../../python/deephy.py*//* https://www.rfc-editor.org/rfc/rfc8949.html */
@Serializable class Test(
  override val name: String,
  override val suffix: String?,
  val images: List<DeephyImage>,
): DeephyObject {


  var model: Model? = null
	get() = field
	set(value) {
	  field = value
	}
  val resolvedImages by lazy { images.mapIndexed { index, im -> ResolvedDeephyImage(im, index, this@Test, model!!) } }

  fun category(id: Int) = images.find { it.categoryID == id }!!.category


}