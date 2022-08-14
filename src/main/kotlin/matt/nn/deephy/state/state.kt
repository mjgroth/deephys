package matt.nn.deephy.state

import kotlinx.serialization.InternalSerializationApi
import matt.hurricanefx.eye.pref.FXPrefNode
import matt.stream.message.FileList

@InternalSerializationApi
object DeephyState: FXPrefNode(
  "sinhalab.deephy",
  oldKeys = listOf(
	"dataFolder",
	"pref"
  )
) {
  val datasets by obj<FileList>()
}