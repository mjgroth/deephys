package matt.nn.deephy.state

import matt.hurricanefx.eye.pref.FXPrefNode
import matt.stream.message.FileList
import matt.stream.message.SFile

object DeephyState: FXPrefNode(
  "sinhalab.deephy",
  oldKeys = listOf(
	"dataFolder",
	"pref",
	"datasets"
  )
) {
  val model by obj<SFile>()
  val tests by obj<FileList>()
  val numImagesPerNeuronInByImage by int(9)
}