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
  val dataFile = dataFolderProperty.relFileBinding("CIFAR10_test".cbor)
  val dataFileV2 = dataFolderProperty.relFileBinding("CIFAR10_test_v2".cbor)
  val dataFileTop = dataFolderProperty.relFileBinding("CIFAR10_test_top".cbor)
  val dataFileTopV2 = dataFolderProperty.relFileBinding("CIFAR10_test_top_v2".cbor)
  val deephyDataFile = dataFolderProperty.relFileBinding("deephyData0".cbor)

  val dataV2File = dataFolderProperty.relFileBinding("CIFAR10v2".cbor)
  val dataV2FileTop = dataFolderProperty.relFileBinding("CIFAR10v2_top".cbor)

  @Suppress("UNUSED_VARIABLE")
  fun load(): Pair<TopV2, ImageV2> {
	//		val top = Cbor.decodeFromByteArray<Top>(dataFileTop.value!!.readBytes())
	//		val data = Cbor.decodeFromByteArray<Image>(dataFile.value!!.readBytes())
	val top = Cbor.decodeFromByteArray<TopV2>(dataFileTopV2.value!!.readBytes())
	val image = Cbor.decodeFromByteArray<ImageV2>(dataFileV2.value!!.readBytes())
	val deephyData = Cbor.decodeFromByteArray<DeephyData>(deephyDataFile.value!!.readBytes())
	return top to image
  }

  fun load2(): Pair<TopV2, ImageV2> {
	val top = Cbor.decodeFromByteArray<TopV2>(dataV2FileTop.value!!.readBytes())
	val image = Cbor.decodeFromByteArray<ImageV2>(dataV2File.value!!.readBytes())
	return top to image
  }
}

@Serializable
class Top(
  val layer_ID: Int,
  val layer_Name: String,
  val num_Neurons: Int,
  val top_100: Array<Array<Int>>
) {
  val layerID get() = layer_ID
  val layerName get() = layer_Name
  val numNeurons get() = num_Neurons
  val top100 get() = top_100
}

@Serializable
class TopV2(
  private val layer_ID: Array<Int>,
  private val layer_Name: Array<String>,
  private val num_Neurons: Array<Int>,
  private val top_100: Array<Array<Array<Int>>>
) {
  val layerID get() = layer_ID[0]
  val layerName get() = layer_Name[0]
  val numNeurons get() = num_Neurons[0]
  val top100 get() = top_100[0]
}


@Serializable
class Image(
  val `file ID`: Int,
  val file_Type: String,
  val category: String,
  val file: Array<Array<Array<Float>>>
)


@Serializable
class ImageV2(
  val `file_ID`: Array<Int>,
  val file_type: Array<String>,
  val category: Array<Int>,
  val file: Array<Array<Array<Array<Double>>>>
)

@Serializable
class GoodImage(
  val fileID: Int,
  val fileType: String,
  val category: Int,
  val matrix: List<List<List<Double>>>
) {
  constructor(image: ImageV2, index: Int): this(
	fileID = image.file_ID[index],
	fileType = image.file_type[index],
	category = image.category[index],
	matrix = image.file[index].let {
	  val newRows = mutableListOf<MutableList<MutableList<Double>>>()
	  it.mapIndexed { colorIndex, singleColorMatrix ->
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
  )
}

@Serializable
class Neuron(
  val index: Int,
  val top100: List<GoodImage>,
) {
  override fun toString(): String {
	return "neuron $index"
  }
}


/* https://www.rfc-editor.org/rfc/rfc8949.html */
@Serializable
class DeephyData(
  val layerID: String,
  val layerName: String,
  val numNeurons: Int,
  val top100: List<List<List<Int>>>,
  val fileIDs: List<Int>,
  val fileTypes: List<Int>,
  val categories: List<Int>,
  val files: List<List<List<List<Double>>>>
)