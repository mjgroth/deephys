package matt.nn.deephys.gui.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import matt.async.thread.ThreadReport
import matt.auto.desktop.awt.AwtBasedDesktopAutomationContext
import matt.compose.state.lang.immutableStateOf
import matt.compose.state.option.SettingsData
import matt.compose.state.save.create.oldNameOldKeysMessage
import matt.compose.state.save.create.stateStructureDatabase
import matt.compose.state.ser.struct.StateStructSerializer
import matt.compose.state.statefulmodel.action.SimpleAction
import matt.compose.state.struct.StateStructure
import matt.file.commons.reg.RegisteredFolder
import matt.file.construct.toJioFile
import matt.lang.err.unsafeError
import matt.log.report.desktop.MemReport
import matt.nn.deephys.gui.DEEPHYS_LOG_FOLDER

fun DeephySettingsNodeNode(scope: CoroutineScope) =
    stateStructureDatabase<DeephySettingsNode>(

        file = RegisteredFolder.Main.preferenceNodeJson("sinhalab.deephys.settings"),
        scope = scope,
        lazyLoad = true,
        autoBackup = false
    )
class DeephySettingsNode : StateStructure() {

    init {
        unsafeError(
            oldNameOldKeysMessage(
                oldNames =
                    listOf(
                        "sinhalab.deephy.settings"
                    ),
                oldKeys =
                    listOf(
                        "normalizeTopNeuronActivations"
                    )
            )
        )
    }

    val settings by registeredSubStateValue {
        DeephysSettingsController()
    }
}

const val MAX_NUM_IMAGES_IN_TOP_NEURONS = 18
const val MAX_NUM_IMAGES_IN_TOP_IMAGES = 100
const val DEFAULT_BIG_IMAGE_SCALE = 128.0

internal object DeephySettingsSerializer: KSerializer<DeephysSettingsController> by StateStructSerializer.createVersioned(
    DeephysSettingsController::class,
    classVersion = 4
)

@Serializable(with = DeephySettingsSerializer::class)
class DeephysSettingsController : SettingsData("Main Settings") {

    val fakeSettingToForceLoading by DoubleSettingProv(
        defaultValue = 1.0,
        label = "fakeSettingToForceLoading",
        tooltip = "fakeSettingToForceLoading",
        min = -10.0,
        max = 10.0
    )

    val appearance by registeredSubStateValue { AppearanceSettings() }

    val millisecondsBeforeTooltipsVanish by IntSettingProv(
        defaultValue = 1000,
        label = "tooltip hide delay (ms)",
        tooltip = "Milliseconds before tooltips vanish. 0 means infinite (hit ESCAPE to make them go away)",
        min = 0,
        max = 10000
    )

    val showTutorials by BoolSettingProv(
        defaultValue = true,
        label = "Show Tutorials",
        tooltip = "Show Interactive Tutorials Throughout the app"
    )

    val debug by registeredSubStateValue { DebugSettings() }
}

class AppearanceSettings : SettingsData("Appearance") {
    val averageRawActSigFigs by IntSettingProv(
        defaultValue = 2,
        label = "average activation significant figures",
        tooltip = "significant figures for top categories list in the Neuron view",
        min = 1,
        max = 10
    )
    val predictionSigFigs by IntSettingProv(
        defaultValue = 5,
        label = "Prediction value significant figures",
        tooltip = "Prediction value significant figures",
        min = 3,
        max = 10
    )
    val numImagesPerNeuronInByImage by IntSettingProv(
        defaultValue = 12,
        label = "Number of images per neuron in top neurons row",
        tooltip = "Number of images per neuron in top neurons row",
        min = 8,
        max = MAX_NUM_IMAGES_IN_TOP_NEURONS
    )
    val smallImageScale by DoubleSettingProv(
        defaultValue = 32.0,
        label = "Small image scale",
        tooltip = "the width (in pixels) for default images",
        min = 10.0,
        max = 100.0
    )
    val bigImageScale by DoubleSettingProv(
        defaultValue = DEFAULT_BIG_IMAGE_SCALE,
        label = "Big image scale",
        tooltip = "the width (in pixels) for big images",
        min = 110.0,
        max = 200.0
    )
}

class DebugSettings : SettingsData("Debug") {

    val showCacheBars by BoolSettingProv(
        defaultValue = false,
        label = "Cache Progress Bars",
        tooltip = "Extra progress bars indicating the progress of data caching."
    )

    @Suppress("unused")
    val verboseLogging by BoolSettingProv(
        defaultValue = false,
        label = "Verbose Logging",
        tooltip = "Extra logging to standard out. May impact performance."
    )

    @Suppress("unused")
    val resetSettings =
        SimpleAction(
            "Reset all settings to default",
            enabled = immutableStateOf(true)
        ) {
            settings.forEach {
                it.resetToDefault()
            }
        }

    @Suppress("unused")
    val deleteState =
        SimpleAction(
            "Delete State",
            enabled = immutableStateOf(true)
        ) {
            unsafeError("DeephyState.delete()")
        }

    @Suppress("unused")
    val printRamInfo =
        SimpleAction(
            "Print RAM info to console",
            enabled = immutableStateOf(true)
        ) {
            println(MemReport())
        }

    @Suppress("unused")
    val printThreadInfo =
        SimpleAction(
            "Print thread info to console",
            enabled = immutableStateOf(true)
        ) {
            println(ThreadReport())
        }

    @Suppress("unused")
    val openLogFolder =
        SimpleAction(
            "Open Log Folder",
            enabled = immutableStateOf(true)
        ) {
            val _ = DEEPHYS_LOG_FOLDER.toJioFile().mkdirs()
            AwtBasedDesktopAutomationContext.showInFileManager(DEEPHYS_LOG_FOLDER)
        }
}
