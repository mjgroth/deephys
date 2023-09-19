package matt.nn.deephys.state

import matt.model.data.message.FileList
import matt.model.data.message.MacFile
import matt.pref.obs.ObsPrefNode

object DeephyState: ObsPrefNode(
  "sinhalab.deephys.state",
  oldNames = listOf(
	"sinhalab.deephy.state"
  ),
  oldKeys = listOf(
	"dataFolder",
	"pref",
	"datasets"
  )
) {
  val model by obj<MacFile>()
  val tests by obj<FileList>()
  val lastVersionOpened by string("")
}


