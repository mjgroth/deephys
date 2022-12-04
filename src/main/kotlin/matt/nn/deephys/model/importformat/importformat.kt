package matt.nn.deephys.model.importformat

import kotlinx.serialization.Serializable
import matt.async.thread.daemon
import matt.collect.weak.lazyWeakMap
import matt.collect.weak.soft.lazySoftMap
import matt.fx.graphics.wrapper.style.FXColor
import matt.lang.weak.lazyWeak
import matt.log.profile.mem.throttle
import matt.log.profile.stopwatch.stopwatch
import matt.model.flowlogic.latch.asyncloaded.DaemonLoadedValueOp
import matt.model.flowlogic.latch.asyncloaded.DelegatedSlot
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.model.obj.single.SingleCall
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.load.test.ActivationData
import matt.nn.deephys.load.test.PixelData3
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.LayerLike
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.ResolvedNeuron
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.state.DeephySettings
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.MultiArray
import org.jetbrains.kotlinx.multik.ndarray.data.slice
import org.jetbrains.kotlinx.multik.ndarray.operations.forEachIndexed
import org.jetbrains.kotlinx.multik.ndarray.operations.max
import kotlin.collections.set

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
  override val layerID: String, val neurons: List<Neuron>
): LayerLike {
  override fun toString() = layerID
}

@Serializable class Neuron

interface TestOrLoader {
  val test: Test
}

/*../../../../../../python/deephy.py*//* https://www.rfc-editor.org/rfc/rfc8949.html */
class Test(
  override val name: String,
  override val suffix: String?,
  val images: List<DeephyImage>,
): DeephyFileObject, TestOrLoader {

  override val test = this

  var model: Model? = null
  var testNeurons: Map<InterTestNeuron, TestNeuron>? = null


  fun category(id: Int) = images.find { it.category.id == id }!!.category

  val categories by lazy {
	stopwatch("categories", enabled = DeephySettings.verboseLogging.value) {
	  images.map { it.category }.toSet().toList().sortedBy { it.id }
	}
  }


  private val imagesByCategoryID by lazy {
	stopwatch("imagesByCategoryID", enabled = DeephySettings.verboseLogging.value) {
	  images.groupBy { it.category.id }.mapValues { it.value.toSet() }
	}
  }

  fun imagesWithGroundTruth(category: Category) = imagesByCategoryID[category.id] ?: setOf()
  fun imagesWithoutGroundTruth(category: Category) = images - (imagesByCategoryID[category.id] ?: setOf())

  val startPreloadingActs = SingleCall {
	daemon("start loading preds") {    /*this makes sense only when you have a machine with infinite ram...*/    /*model!!.layers.forEachIndexed { index, _ ->
		activationsMatByLayerIndex[index]
	  }*/
	  preds.startLoading()
	}
  }

  private val activationsMatByLayerIndex = lazyWeakMap<Int, D2Array<Float>> { lay ->
	images.map {
	  it.weakActivations[lay].asList()
	}.toNDArray()
  }


  val activationsByNeuron = lazyWeakMap<InterTestNeuron, MultiArray<Float, D1>> {

	testNeurons!![it]!!.activations.await().asList().toNDArray()


	/*val myMat = activationsMatByLayerIndex[it.layer.index]
	myMat[0 until myMat.shape[0], it.index]*/


  }


  val maxActivations = lazySoftMap<InterTestNeuron, Float> { neuron ->
	activationsMatByLayerIndex[neuron.layer.index].slice<Float, D2, D1>(neuron.index..neuron.index, axis = 1).max()!!
  }


  val preds = DaemonLoadedValueOp<Map<DeephyImage, Category>> {
	val m = mutableMapOf<DeephyImage, Category>()
	val clsLayIdx = model!!.classificationLayer.index
	val chunkSize = 1000
	images.chunked(chunkSize).forEachIndexed { chunkIndex, imageChunk ->
	  val actsMat = imageChunk.map {
		it.weakActivations[clsLayIdx].asList()
	  }.toNDArray()
	  val argMaxResults = mk.math.argMaxD2(actsMat, 1)
	  val imageStartIndex = chunkIndex*chunkSize
	  argMaxResults.forEachIndexed { imageIndex, predIndex ->
		m[images[imageStartIndex + imageIndex]] = category(predIndex)
	  }
	  throttle("preds of $name")
	}

	/*val argMaxResults = mk.math.argMaxD2(activationsMatByLayerIndex[model!!.classificationLayer.index], 1)
	argMaxResults.forEachIndexed { imageIndex, predIndex ->
	  m[images[imageIndex]] = category(predIndex)
	}*/

	m
  }


}


class TestNeuron(val index: Int, val layerIndex: Int) {
  val activations = DelegatedSlot<FloatArray>()
}

class DeephyImage(
  val imageID: Int,
  categoryID: Int,
  category: String,
  val activations: ModelState,
  val testLoader: TestLoader,
  val index: Int,
  val model: Model,
  val test: LoadedValueSlot<Test>,
  val features: Map<String, String>?
) {


  val category = Category(id = categoryID, label = category)

  val matrix by lazyWeak {
	val d = data.await()
	val numRows = d[0].size
	val numCols = d[0][0].size

	(0 until numRows).map { index1 ->
	  MutableList<FXColor>(numCols) { index2 ->
		FXColor.rgb(d[0][index1][index2], d[1][index1][index2], d[2][index1][index2])
	  }
	}
  }


  internal val weakActivations by lazyWeak {
	activations.activations.await()
  }

  fun activationsFor(rLayer: InterTestLayer): FloatArray = weakActivations[rLayer.index]
  fun activationFor(neuron: InterTestNeuron) = RawActivation(weakActivations[neuron.layer.index][neuron.index])

  val data = DelegatedSlot<PixelData3>()

  val prediction by lazy {
	test.await().preds.await()[this]!!
  }

}

class ModelState(

) {
  val activations = DelegatedSlot<ActivationData>()
}




