@file:OptIn(ExperimentalUnsignedTypes::class)

package matt.nn.deephy.model

import kotlinx.serialization.Serializable
import matt.collect.map.lazyMap
import matt.hurricanefx.wrapper.style.FXColor
import matt.model.latch.asyncloaded.AsyncLoadingValue
import matt.nn.deephy.state.DeephySettings
import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.slice
import org.jetbrains.kotlinx.multik.ndarray.operations.max
import org.jetbrains.kotlinx.multik.ndarray.operations.toList


sealed interface NeuronRef {
  val neuron: Neuron
}

data class InterTestLayer(
  val index: Int
)

data class InterTestNeuron(
  val layer: InterTestLayer,
  val index: Int
)

@Serializable class Neuron: NeuronRef {
  override val neuron get() = this
}


class NeuronTestResults(
  override val neuron: Neuron,
  private val activations: List<Float>
): NeuronRef {


  val activationIndexesHighToLow by lazy {
	//	println("activationIndexesHighToLow activations.size=${activations.size}")
	activations.withIndex().sortedBy { it.value }.reversed().map { it.index }
  }
}

interface ResolvedNeuronLike: NeuronRef {
  val index: Int
  val layer: ResolvedLayer
  val interTest: InterTestNeuron
}


class ResolvedNeuron(
  override val neuron: Neuron,
  override val index: Int,
  override val layer: ResolvedLayer,
): ResolvedNeuronLike {
  override val interTest by lazy { InterTestNeuron(layer.interTest, index) }
}


class NeuronWithActivation(
  val rNeuron: ResolvedNeuron,
  val activation: Float,
  val normalizedActivation: Float
): ResolvedNeuronLike by rNeuron
//
//interface DeephyImageData {
//  val data: List<List<List<Double>>>
//  val matrix: List<List<List<Double>>>
//  val activations: ModelState
//}


class ModelState(

) {
  val activations = AsyncLoadingValue<List<FloatArray>>()
}


class DeephyImage(
  val imageID: Int,
  val categoryID: Int,
  val category: String,
  //  override val data: List<List<List<Double>>>,
  val activations: ModelState
) {


  val matrix by lazy {


	//	val newRows = mutableListOf<MutableList<FXColor>>()


	val d = data.await()
	val numRows = d[0].size
	val numCols = d[0][0].size

	val r = (0 until numRows).map { index1 ->
	  MutableList<FXColor>(numCols) { index2 ->
		FXColor.rgb(d[0][index1][index2].toInt(), d[1][index1][index2].toInt(), d[2][index1][index2].toInt())
	  }
	}

	r
	//
	//
	//	data.await().mapIndexed { colorIndex, singleColorMatrix ->
	//
	//
	//	  singleColorMatrix.mapIndexed { rowIndex, row ->
	//
	//
	//		val newRow =
	//		  if (colorIndex == 0) mutableListOf<FXColor>().also { newRows += it } else newRows[rowIndex]
	//
	//		row.mapIndexed { colIndex, pixel ->
	//
	//
	//		  val newCol = if (colorIndex == 0) mutableListOf<FXColor>().also { newRow += it } else newRow[colIndex]
	//		  newCol += pixel.toInt()
	//		}
	//	  }
	//
	//
	//	}
	//	newRows
  }


  internal val goodActivations by lazy {
	activations.activations.await()
  }

  fun activationsFor(rLayer: ResolvedLayer): FloatArray = goodActivations[rLayer.index]
  fun activationFor(neuron: ResolvedNeuron) = goodActivations[neuron.layer.index][neuron.index]

  val test = AsyncLoadingValue<Test>()
  val index = AsyncLoadingValue<Int>()
  val model = AsyncLoadingValue<Model>()
  val data = AsyncLoadingValue<List<List<IntArray>>>()

  fun topNeurons(rLayer: ResolvedLayer): List<NeuronWithActivation> {
	//	val t = tic("topNeurons")
	//	t.toc("1")
	val r = activationsFor(rLayer).run {
	  //	  t.toc("2")

	  val tst = test.await()

	  /*val mx = max()*/
	  mapIndexed { neuronIndex, a ->
		//		val sub = tic("top neurons sub")
		//		sub.toc("1")
		val neuron = rLayer.neurons[neuronIndex]
		//		sub.toc("2")

		val maxActivationForAllImagesForThisNeuron = tst.maxActivations[neuron.interTest]
		//		sub.toc("3")
		val rr = NeuronWithActivation(
		  rLayer.neurons[neuronIndex],
		  a,
		  normalizedActivation = a/maxActivationForAllImagesForThisNeuron /*mx*/
		)
		//		sub.toc("4")
		rr
	  }
		.sortedBy { if (DeephySettings.normalizeTopNeuronActivations.value) it.normalizedActivation else (it.activation) }
		.reversed()
		.take(25)
	}
	//	t.toc("3")
	return r
  }

}


val normalizeTopNeuronsBlurb = """
  Activations of top neurons are normalized by dividing the raw activation by the maximum activations for all images for this neuron
""".trimIndent()


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
  val interTest by lazy { InterTestLayer(index) }
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
class Test(
  override val name: String,
  override val suffix: String?,
  val images: List<DeephyImage>,
): DeephyObject {


  var model: Model? = null


  fun category(id: Int) = images.find { it.categoryID == id }!!.category

  val activationsMatByLayerIndex = lazyMap<Int, D2Array<Float>> { lay ->
	//	val t = tic("activationsMat")
	//	t.toc("1")
	val r = images.map { it.goodActivations[lay].toList() }.toNDArray()
	//	var r: D3Array<Float>? = null
	//	for (i in images) {
	//	  val a = i.activations.activations.await()
	//	  if (r == null) {
	//		r = a.toNDArray().asD3Array()
	//		println("first r shape = ${r!!.shape.joinToString(",")}")
	//	  } else r = r!!.cat(a.toNDArray().asD3Array().also {
	//		println("first trying to add to it shape = ${it.shape.joinToString(",")}")
	//	  })
	//	}
	//	t.toc("2")
	r
  }

  val activationsByNeuron = lazyMap<ResolvedNeuron, List<Float>> {
	//	println("activationsMatByLayerIndex.size=${activationsMatByLayerIndex.size}")
	//	println("activationsMatByLayerIndex[0].shape=${activationsMatByLayerIndex[0].shape.joinToString()}")
	//	println("activationsMatByLayerIndex[0][0].size=${activationsMatByLayerIndex[0][0].size}")

	val myMat = activationsMatByLayerIndex[it.layer.index]
	//	println("myMat.shape=${myMat.shape.joinToString()}")
	//	println("slice1.shape=${slice1.shape.joinToString()}")
	val slice2 = myMat[0 until myMat.shape[0], it.index]
	//	println("slice2.shape=${slice2.shape.joinToString()}")
	slice2.toList()

	//	val layerIndex = it.first
	//	val neuronIndex = it.second
	//	images.map { it.activations.activations.await()[layerIndex][neuronIndex] }

  }


  val maxActivations = lazyMap<InterTestNeuron, Float> { neuron ->
	activationsMatByLayerIndex[neuron.layer.index].slice<Float, D2, D1>(neuron.index..neuron.index, axis = 1).max()!!
  }


}