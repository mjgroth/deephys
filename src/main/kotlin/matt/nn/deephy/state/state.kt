package matt.nn.deephy.state

import matt.hurricanefx.eye.pref.FXPrefNode
import matt.stream.message.FileList

object DeephyState: FXPrefNode(
  "sinhalab.deephy",
  oldKeys = listOf(
	"dataFolder",
	"pref"
  )
) {
  val datasets by obj<FileList>()
}