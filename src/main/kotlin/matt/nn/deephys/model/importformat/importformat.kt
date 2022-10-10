package matt.nn.deephys.model.importformat

import kotlinx.serialization.Serializable
import matt.async.thread.daemon
import matt.collect.map.lazyMap
import matt.fx.graphics.wrapper.style.FXColor
import matt.log.profile.tic
import matt.model.latch.asyncloaded.DaemonLoadedValueOp
import matt.model.latch.asyncloaded.LoadedValueSlot
import matt.model.obj.single.SingleCall
import matt.nn.deephys.model.LayerLike
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.ResolvedNeuron
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.MultiArray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.slice
import org.jetbrains.kotlinx.multik.ndarray.operations.forEachIndexed
import org.jetbrains.kotlinx.multik.ndarray.operations.max

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

  val startPreloadingActs = SingleCall {
	val t = tic("preloading acts")
	t.toc(0)
	daemon {
	  t.toc(1)
	  model!!.layers.forEachIndexed { index, _ ->
		activationsMatByLayerIndex[index]
	  }
	  t.toc(2)


	  preds.startLoading()
	  t.toc(3)
	}
	t.toc(4)


  }

  val activationsMatByLayerIndex = lazyMap<Int, D2Array<Float>> { lay ->
	val r = images.map { it.goodActivations[lay].toList() }.toNDArray()
	r
  }

  val activationsByNeuron = lazyMap<InterTestNeuron, MultiArray<Float, D1>> {
	val myMat = activationsMatByLayerIndex[it.layer.index]
	myMat[0 until myMat.shape[0], it.index]
  }


  val maxActivations = lazyMap<InterTestNeuron, Float> { neuron ->
	activationsMatByLayerIndex[neuron.layer.index].slice<Float, D2, D1>(neuron.index..neuron.index, axis = 1).max()!!
  }


  val preds = DaemonLoadedValueOp<Map<DeephyImage, Category>> {
	val m = mutableMapOf<DeephyImage, Category>()
	mk.math.argMaxD2(activationsMatByLayerIndex[model!!.classificationLayer.index], 1)
	  .forEachIndexed { imageIndex, predIndex ->
		m[images[imageIndex]] = category(predIndex)
	  }
	m
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

  val test = LoadedValueSlot<Test>()
  val index = LoadedValueSlot<Int>()
  val model = LoadedValueSlot<Model>()
  val data = LoadedValueSlot<List<List<IntArray>>>()

}

class ModelState(

) {
  val activations = LoadedValueSlot<List<FloatArray>>()
}




