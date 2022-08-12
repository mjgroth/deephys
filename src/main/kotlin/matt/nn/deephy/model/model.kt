@file:Suppress("OPT_IN_USAGE")

package matt.nn.deephy.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import matt.file.MFile
import matt.file.cbor
import matt.file.construct.mFile
import matt.hurricanefx.eye.lang.Prop
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.relFileBinding
import matt.nn.deephy.pref.Pref

object DeephyDataManager {
  private var dataFolder by Pref()
  val dataFolderProperty by lazy {
	Prop<MFile?>(dataFolder?.let { mFile(it) }).apply {
	  onChange {
		dataFolder = it?.abspath
	  }
	}
  }

  val cifarV1Test = dataFolderProperty.relFileBinding("CIFARV1_test".cbor)

  fun load3(): DeephyData = Cbor.decodeFromByteArray(cifarV1Test.value!!.readBytes())
}

@Serializable
class Neuron(
  val activations: List<Double>
) {
  val activationIndexesHighToLow by lazy {
	activations.withIndex().sortedBy { it.value }.reversed().map { it.index }
  }
}

@Serializable
class DeephyImage(
  val imageID: Int,
  val categoryID: Int,
  val category: String,
  val data: List<List<List<Double>>>
) {

  val matrix by lazy {
	val newRows = mutableListOf<MutableList<MutableList<Double>>>()
	data.mapIndexed { colorIndex, singleColorMatrix ->
	  singleColorMatrix.mapIndexed { rowIndex, row ->
		val newRow =
		  if (colorIndex == 0) mutableListOf<MutableList<Double>>().also { newRows += it } else newRows[rowIndex]

		row.mapIndexed { colIndex, pixel ->
		  val newCol =
			if (colorIndex == 0) mutableListOf<Double>().also { newRow += it } else newRow[colIndex]
		  newCol += pixel
		}
	  }
	}
	newRows
  }
}

@Serializable
class Layer(
  val layerID: String,
  val neurons: List<Neuron>
)


/*../../../../../../python/deephy.py*/
/* https://www.rfc-editor.org/rfc/rfc8949.html */
@Serializable
class DeephyData(
  val datasetName: String,
  val suffix: String?,
  val images: List<DeephyImage>,
  val layers: List<Layer>,
)