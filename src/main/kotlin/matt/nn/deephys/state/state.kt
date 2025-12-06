package matt.nn.deephys.state

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import matt.compose.state.save.create.oldNameOldKeysMessage
import matt.compose.state.save.create.stateStructureDatabase
import matt.compose.state.struct.StateStructure
import matt.file.commons.reg.RegisteredFolder
import matt.lang.anno.optin.ExperimentalMattCode
import matt.lang.common.unsafeError
import matt.model.data.message.AbsLinuxFile
import matt.nn.deephys.load.CborSyncLoadResult
import matt.nn.deephys.model.importformat.Model

fun DeephyStateDb(scope: CoroutineScope) =
    stateStructureDatabase<DeephyState>(
        file = RegisteredFolder.Main.preferenceNodeJson("sinhalab.deephys.state"),
        scope = scope,
        lazyLoad = true,
        autoBackup = false
    )
@OptIn(ExperimentalMattCode::class)
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
    /*yes, this is not to be serialized*/
    val loadedModel = mutableStateOf<CborSyncLoadResult<Model>?>(null)
}
