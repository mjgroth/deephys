package matt.nn.deephy.model

import kotlinx.serialization.Serializable

@Serializable
class Top(
  val layer_ID: Int,
  val layer_Name: String,
  val num_Neurons: Int,
  val top_100: Array<Array<Int>>
)

@Serializable
class Image(
  val `file ID`: String,
  val file_Type: String,
  val category: String,
  val file: Array<Array<Array<Float>>>
)
