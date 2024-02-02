package matt.nn.deephys.state

import matt.model.data.message.AbsLinuxFile
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
    val model by obj<AbsLinuxFile>()
    val tests by obj<List<AbsLinuxFile>>()
    val lastVersionOpened by string("")
}


