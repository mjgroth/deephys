package matt.nn.deephys.model.importformat

import com.google.common.collect.MapMaker
import kotlinx.serialization.Serializable
import matt.collect.dmap.withStoringDefault
import matt.collect.weak.lazyWeakMap
import matt.collect.weak.soft.lazySoftMap
import matt.log.profile.mem.throttle
import matt.log.profile.stopwatch.stopwatch
import matt.model.flowlogic.latch.asyncloaded.DaemonLoadedValueOp
import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.testcache.TestRAMCache
import matt.nn.deephys.model.ResolvedLayer
import matt.nn.deephys.model.ResolvedNeuron
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.layer.Layer
import matt.nn.deephys.model.importformat.neuron.TestNeuron
import matt.nn.deephys.model.importformat.testlike.TestOrLoader
import matt.nn.deephys.state.DeephySettings
import matt.prim.str.mybuild.string
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.MultiArray
import org.jetbrains.kotlinx.multik.ndarray.operations.forEachIndexed
import org.jetbrains.kotlinx.multik.ndarray.operations.max
import java.lang.ref.WeakReference
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

  fun infoString() = string {
	lineDelimited {
	  +"Model:"
	  +"\tname=$name"
	  +"\tsuffix=$suffix"
	  +"\tlayers:"
	  layers.forEach {
		+"\t\t${it.layerID} (${it.neurons.size} neurons)"
	  }
	}
  }


}

/*../../../../../../python/deephy.py*//* https://www.rfc-editor.org/rfc/rfc8949.html */
class Test(
  override val name: String,
  override val suffix: String?,
  val images: List<DeephyImage<*>>,
  val model: Model,
  override val testRAMCache: TestRAMCache,
  override val dtype: DType<*>
): DeephyFileObject, TestOrLoader {

  override val test = this


  var testNeurons: Map<InterTestNeuron, TestNeuron>? = null


  fun category(id: Int) = images.find { it.category.id == id }!!.category

  val categories by lazy {
	stopwatch("categories", enabled = DeephySettings.verboseLogging.value) {
	  images.map { it.category }.toSet().toList().sortedBy { it.id }
	}
  }
  val catsByID by lazy {
	categories.associateBy { it.id }
  }


  private val imagesByCategoryID by lazy {
	stopwatch("imagesByCategoryID", enabled = DeephySettings.verboseLogging.value) {
	  images.groupBy { it.category.id }.mapValues { it.value.toSet() }
	}
  }

  fun imagesWithGroundTruth(category: Category) = imagesByCategoryID[category.id] ?: setOf()
  fun imagesWithoutGroundTruth(category: Category) = images - (imagesByCategoryID[category.id] ?: setOf())


  private val activationsMatByLayerIndex = lazyWeakMap<Int, D2Array<Float>> { lay ->
	images.map {
	  it.weakActivations[lay].asList()
	}.toNDArray()
  }


  val activationsByNeuron = MapMaker()
	.weakKeys().apply {

	}
	.weakValues()
	.makeMap<InterTestNeuron, MultiArray<Float, D1>>()
	.withStoringDefault {
	  testNeurons!![it]!!.activations.await().asList().toNDArray()
	  /*val myMat = activationsMatByLayerIndex[it.layer.index]
myMat[0 ..< myMat.shape[0], it.index]*/
	}

  /*	lazyWeakMap<InterTestNeuron, MultiArray<Float, D1>> {


	  //	error("todo: fix this memory leak")


	  testNeurons!![it]!!.activations.await().asList().toNDArray()


	  //	WeakReference(actData)





	}*/


  val maxActivations = lazySoftMap<InterTestNeuron, Float> { neuron ->

	/*activationsMatByLayerIndex[neuron.layer.index].slice<Float, D2, D1>(neuron.index..neuron.index, axis = 1).max()!!*/

	activationsByNeuron[neuron].max()!!

  }

  val preds = run {
	val clsLayerIndex = model.classificationLayer.index /*attempt to remove ref to Test from thread below*/
	val ims = images
	val nam = name
	val weakTest = WeakReference(test)
	DaemonLoadedValueOp<Map<DeephyImage, Category>> {
	  val localCatsByID = weakTest.get()!!.catsByID
	  val m = HashMap<DeephyImage, Category>(ims.size)
	  val chunkSize = 1000
	  ims.chunked(chunkSize).forEachIndexed { chunkIndex, imageChunk ->
		val actsMat = imageChunk.map {
		  it.weakActivations[clsLayerIndex].asList()
		}.toNDArray()
		val argMaxResults = mk.math.argMaxD2(actsMat, 1)
		val imageStartIndex = chunkIndex*chunkSize
		argMaxResults.forEachIndexed { imageIndex, predIndex ->
		  val im = ims[imageStartIndex + imageIndex]
		  m[im] = localCatsByID[predIndex] ?: error(
			string {
			  lineDelimited {
				+"could not find category for predIndex=$predIndex (${localCatsByID.size} categories) of Image[index=${im.index}]"

				+"image categories:"
				ims.forEach {
				  +"\t${it.category.id}"
				}

			  }

			}
		  )
		}
		throttle("preds of $nam")
	  }

	  /*val argMaxResults = mk.math.argMaxD2(activationsMatByLayerIndex[model!!.classificationLayer.index], 1)
	  argMaxResults.forEachIndexed { imageIndex, predIndex ->
		m[images[imageIndex]] = category(predIndex)
	  }*/

	  m
	}
  }
}


