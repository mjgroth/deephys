package matt.nn.deephy.state

import matt.hurricanefx.eye.pref.FXPrefNode
import matt.stream.message.SFile

object DeephyState: FXPrefNode(
  "sinhalab.deephy",
  oldKeys = listOf(
	"dataFolder"
  )
) {
  val datasets by obj<List<SFile>>()
}