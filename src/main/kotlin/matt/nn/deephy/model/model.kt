package matt.nn.deephy.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import matt.file.MFile
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.target.EventTargetWrapper
import matt.hurricanefx.wrapper.text.TextWrapper
import matt.obs.prop.MObservableROValBase

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


sealed interface NeuronRef {
  val neuron: Neuron
  //  val activations: List<Double>
  //  val index: Int
  //  val layer: LayerLike
  //  val activationIndexesHighToLow: List<Int>
}


@Serializable class Neuron(
  //  override val activations: List<Double>
): NeuronRef {

  //  override val index get() = NOT_IMPLEMENTED
  //  override val layer get() = NOT_IMPLEMENTED
  override val neuron get() = this
}


class NeuronTestResults(
  override val neuron: Neuron,
  val activations: List<Double>
): NeuronRef {



  init {
	println("NeuronTestResults.acitvations.size=${activations.size}")
  }

  val activationIndexesHighToLow by lazy {
	require(activations.all { it >= 0.0 })
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


class NeuronWithActivation(val rNeuron: ResolvedNeuron, val activation: Double): ResolvedNeuronLike by rNeuron

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


  fun topNeurons(): List<NeuronWithActivation> =
	activations.activations
	  .flatMapIndexed { layerIndex, layer ->
		val rLay = model.resolvedLayers[layerIndex]
		layer.mapIndexed { neuronIndex, a ->
		  NeuronWithActivation(rLay.neurons[neuronIndex], a)
		}
	  }
	  .sortedBy { it.activation }
	  .reversed()
	  .take(25)
}

interface LayerLike {
  val layerID: String
  val neurons: List<NeuronRef>
  override fun toString(): String
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
}

class FileNotFound<T>(val f: MFile): CborTestLoadResult<T>
class ParseError<T>(val message: String?): CborTestLoadResult<T>
class Loaded<T>(val data: T): CborTestLoadResult<T>

sealed interface CborTestLoadResult<T>

inline fun <reified T: Any> MFile.loadCbor(): CborTestLoadResult<T> = if (doesNotExist) FileNotFound(this) else try {
  Loaded(Cbor.decodeFromByteArray(readBytes()))
} catch (e: SerializationException) {
  ParseError(e.message)
}

fun <T> EventTargetWrapper.loadSwapper(
  prop: MObservableROValBase<CborTestLoadResult<T>?>,
  nullMessage: String = "please select a file",
  op: T.()->NodeWrapper
) = swapper(prop, nullMessage) {
  when (this) {
	is FileNotFound -> TextWrapper("$f not found")
	is ParseError   -> TextWrapper("parse error: $message")
	is Loaded<T>    -> op(this.data)
  }
}