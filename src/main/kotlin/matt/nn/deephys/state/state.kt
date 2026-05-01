package matt.nn.deephys.state

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import matt.compose.state.save.create.oldNameOldKeysMessage
import matt.compose.state.save.create.stateStructureDatabase
import matt.compose.state.struct.StateStructure
import matt.file.common.toAbsLinuxFile
import matt.file.commons.reg.RegisteredFolder
import matt.file.construct.toJioFile
import matt.lang.anno.optin.ExperimentalMattCode
import matt.lang.err.unsafeError
import matt.model.k.file.file.FsFile
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
import matt.nn.deephys.load.CborSyncLoadResult
import matt.nn.deephys.model.importformat.Model
import matt.osi.serfile.AbsLinuxFile

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

fun load(
    modelFile: FsFile,
    testFiles: List<FsFile>,
    deephyState: DeephyState,
    dsetViewsState: DSetViewsState
) {
    deephyState.model.value = modelFile.toAbsLinuxFile()
    dsetViewsState.removeAllTests()
    testFiles.forEach { f ->
        val viewer = dsetViewsState.addTest()
        viewer.setCborFile(f.toJioFile())
    }
}
