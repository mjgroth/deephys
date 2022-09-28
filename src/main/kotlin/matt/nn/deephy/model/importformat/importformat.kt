package matt.nn.deephy.model.importformat

import kotlinx.serialization.Serializable
import matt.collect.map.lazyMap
import matt.hurricanefx.wrapper.style.FXColor
import matt.model.latch.asyncloaded.AsyncLoadingValue
import matt.nn.deephy.model.LayerLike
import matt.nn.deephy.model.ResolvedLayer
import matt.nn.deephy.model.ResolvedNeuron
import matt.nn.deephy.model.data.Category
import matt.nn.deephy.model.data.InterTestLayer
import matt.nn.deephy.model.data.InterTestNeuron
import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.slice
import org.jetbrains.kotlinx.multik.ndarray.operations.max
import org.jetbrains.kotlinx.multik.ndarray.operations.toList

sealed interface DeephyFileObject {
  val name: String
  val suffix: String?
}

@Serializable class Model(
  override val name: String,
  override val suffix: String?,
  val layers: List<Layer>,
): DeephyFileObject {
  val resolvedLayers by lazy { layers.mapIndexed { index, layer -> ResolvedLayer(layer, this@Model, index) } }
  val neurons: List<ResolvedNeuron> by lazy { resolvedLayers.flatMap { it.neurons } }
  val classificationLayer by lazy {
	resolvedLayers.first { it.isClassification }
  }
}

@Serializable class Layer(
  override val layerID: String,
  val neurons: List<Neuron>
): LayerLike {
  override fun toString() = layerID
}

@Serializable class Neuron


/*../../../../../../python/deephy.py*//* https://www.rfc-editor.org/rfc/rfc8949.html */
class Test(
  override val name: String,
  override val suffix: String?,
  val images: List<DeephyImage>,
): DeephyFileObject {


  var model: Model? = null


  fun category(id: Int) = images.find { it.category.id == id }!!.category

  val categories by lazy {
	images.map { it.category }.toSet().toList().sortedBy { it.id }
  }

  fun imagesWithGroundTruth(category: Category) = images.filter { it.category == category }
  fun imagesWithoutGroundTruth(category: Category) = images.filter { it.category != category }

  val activationsMatByLayerIndex = lazyMap<Int, D2Array<Float>> { lay ->
	val r = images.map { it.goodActivations[lay].toList() }.toNDArray()
	r
  }

  val activationsByNeuron = lazyMap<InterTestNeuron, List<Float>> {
	val myMat = activationsMatByLayerIndex[it.layer.index]
	val slice2 = myMat[0 until myMat.shape[0], it.index]
	slice2.toList()
  }


  val maxActivations = lazyMap<InterTestNeuron, Float> { neuron ->
	activationsMatByLayerIndex[neuron.layer.index].slice<Float, D2, D1>(neuron.index..neuron.index, axis = 1).max()!!
  }


}

class DeephyImage(
  val imageID: Int,
  categoryID: Int,
  category: String,
  val activations: ModelState
) {


  val category = Category(id = categoryID, label = category)

  val matrix by lazy {
	val d = data.await()
	val numRows = d[0].size
	val numCols = d[0][0].size

	val r = (0 until numRows).map { index1 ->
	  MutableList<FXColor>(numCols) { index2 ->
		FXColor.rgb(d[0][index1][index2], d[1][index1][index2], d[2][index1][index2])
	  }
	}
	r
  }


  internal val goodActivations by lazy {
	activations.activations.await()
  }

  fun activationsFor(rLayer: InterTestLayer): FloatArray = goodActivations[rLayer.index]
  fun activationFor(neuron: InterTestNeuron) = goodActivations[neuron.layer.index][neuron.index]

  val test = AsyncLoadingValue<Test>()
  val index = AsyncLoadingValue<Int>()
  val model = AsyncLoadingValue<Model>()
  val data = AsyncLoadingValue<List<List<IntArray>>>()

}

class ModelState(

) {
  val activations = AsyncLoadingValue<List<FloatArray>>()
}




