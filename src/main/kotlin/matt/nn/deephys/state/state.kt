package matt.nn.deephys.state

import kotlinx.coroutines.CoroutineScope
import matt.compose.state.save.create.oldNameOldKeysMessage
import matt.compose.state.save.create.stateStructureDatabase
import matt.compose.state.struct.StateStructure
import matt.file.commons.reg.RegisteredFolder
import matt.lang.common.unsafeError
import matt.model.data.message.AbsLinuxFile


fun DeephyStateDb(scope: CoroutineScope) =
    stateStructureDatabase<DeephyState>(
        file = RegisteredFolder.Main.preferenceNodeJson("sinhalab.deephys.state"),
        scope = scope,
        lazyLoad = true,
        autoBackup = false
    )
class DeephyState: StateStructure() {
    init {
        unsafeError(
            oldNameOldKeysMessage(
                oldNames =
                    listOf(
                        "sinhalab.deephy.state"
                    ),
                oldKeys =
                    listOf(
                        "dataFolder",
                        "pref",
                        "datasets"
                    )
            )
        )
    }
    val model by registeredState<AbsLinuxFile?> { null }
    val tests by registeredState<List<AbsLinuxFile>?> { null }
    val lastVersionOpened by registeredState<String> { "" }
}


