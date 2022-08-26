package matt.nn.deephy.model

import kotlinx.serialization.Serializable
import matt.klib.lang.NOT_IMPLEMENTED

//object DeephyDataManager {
//  private var dataFolder by Pref()
//  val dataFolderProperty by lazy {
//	Prop<MFile?>(dataFolder?.let { mFile(it) }).apply {
//	  onChange {
//		dataFolder = it?.abspath
//	  }
//	}
//  }
//}

sealed interface NeuronLike {
  val activations: List<Double>
  val index: Int
  val layer: LayerLike
  val activationIndexesHighToLow: List<Int>
}


@Serializable class Neuron(
  override val activations: List<Double>
): NeuronLike {
  override val activationIndexesHighToLow by lazy {
	require(activations.all { it >= 0.0 })
	activations.withIndex().sortedBy { it.value }.reversed().map { it.index }
  }
  override val index get() = NOT_IMPLEMENTED
  override val layer get() = NOT_IMPLEMENTED
}


class ResolvedNeuron(
  neuron: Neuron,
  override val index: Int,
  override val layer: ResolvedLayer,
): NeuronLike by neuron


class NeuronWithActivation(val neuron: ResolvedNeuron, val activation: Double): NeuronLike by neuron

interface DeephyImageData {
  val data: List<List<List<Double>>>
  val matrix: List<List<List<Double>>>
}

@Serializable class DeephyImage(
  val imageID: Int, val categoryID: Int, val category: String, override val data: List<List<List<Double>>>
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
  image: DeephyImage, val index: Int, val dataset: Dataset
): DeephyImageData {
  val imageID = image.imageID
  val categoryID = image.categoryID
  val category = image.category
  override val data = image.data
  override val matrix by lazy { image.matrix }


  fun topNeurons() = dataset
	.neurons
	.map {
	  NeuronWithActivation(it, it.activations[index])
	}
	.sortedBy { it.activation }
	.reversed()
	.take(25)
}

interface LayerLike {
  val layerID: String
  val neurons: List<NeuronLike>
  override fun toString(): String
}

@Serializable class Layer(
  override val layerID: String, override val neurons: List<Neuron>
): LayerLike {
  override fun toString() = layerID
}

class ResolvedLayer(
  layer: Layer, val dataset: Dataset
): LayerLike by layer {
  override val neurons = layer.neurons.mapIndexed { index, neuron ->
	ResolvedNeuron(
	  neuron = neuron, index = index, layer = this@ResolvedLayer
	)
  }
}


/*../../../../../../python/deephy.py*//* https://www.rfc-editor.org/rfc/rfc8949.html */
@Serializable class Dataset(
  val datasetName: String,
  val suffix: String?,
  val images: List<DeephyImage>,
  val layers: List<Layer>,
): CborLoadResult {
  val resolvedLayers by lazy { layers.map { ResolvedLayer(it, this@Dataset) } }
  val neurons by lazy { resolvedLayers.flatMap { it.neurons } }
  val resolvedImages by lazy { images.mapIndexed { index, im -> ResolvedDeephyImage(im, index, this@Dataset) } }
}

object FileNotFound: CborLoadResult
object ParseError: CborLoadResult

sealed interface CborLoadResult