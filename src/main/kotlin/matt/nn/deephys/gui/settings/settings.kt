package matt.nn.deephys.gui.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import matt.async.thread.ThreadReport
import matt.gui.option.SettingsData
import matt.lang.file.toJFile
import matt.lang.assertions.require.requireNull
import matt.log.report.MemReport
import matt.nn.deephys.gui.DEEPHYS_LOG_CONTEXT
import matt.nn.deephys.state.DeephyState
import matt.obs.hold.extra.VersionedTypedObsHolderSerializer
import matt.pref.obs.ObsPrefNode
import matt.prim.str.elementsToString
import java.awt.Desktop


class DeephySettingsNode : ObsPrefNode(
    "sinhalab.deephys.settings",
    oldNames = listOf(
        "sinhalab.deephy.settings"
    ),
    oldKeys = listOf(
        "normalizeTopNeuronActivations",
    ),
    json = Json {
        ignoreUnknownKeys = true
    }
) {
    companion object {
        private var instance: DeephySettingsNode? = null
    }

    init {
        synchronized(DeephySettingsNode::class) {
            requireNull(instance)
            instance = this
        }
    }

    val settings by obsObj {
        DeephysSettingsController()
    }

}

const val MAX_NUM_IMAGES_IN_TOP_NEURONS = 18
const val MAX_NUM_IMAGES_IN_TOP_IMAGES = 100
const val DEFAULT_BIG_IMAGE_SCALE = 128.0

private object DeephySettingsSerializer : VersionedTypedObsHolderSerializer<DeephysSettingsController>(
    DeephysSettingsController::class,
    4
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

    val appearance by registeredSection(AppearanceSettings())

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


    val debug by registeredSection(DebugSettings())

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

    val verboseLogging by BoolSettingProv(
        defaultValue = false,
        label = "Verbose Logging",
        tooltip = "Extra logging to standard out. May impact performance."
    )

    val resetSettings = actionNotASetting(
        label = "Reset all settings to default",
        tooltip = "Reset all settings to default",
    ) {
        settings.forEach {
            it.resetToDefault()
        }
    }

    val deleteState = actionNotASetting(
        label = "Delete State",
        tooltip = "Delete State",
    ) {
        DeephyState.delete()
        println("model=${DeephyState.model.value}")
        println("tests=${DeephyState.tests.value?.elementsToString()}")
    }


    val printRamInfo = actionNotASetting(
        label = "Print RAM info to console",
        tooltip = "Print RAM info to console",
    ) {
        println(MemReport())
    }


    val printThreadInfo = actionNotASetting(
        label = "Print thread info to console",
        tooltip = "Print thread info to console",
    ) {
        println(ThreadReport())
    }

    val openLogFolder = actionNotASetting(
        label = "Open Log Folder",
        tooltip = "Open Log Folder",
    ) {
        Desktop.getDesktop().browseFileDirectory(DEEPHYS_LOG_CONTEXT.logFolder.toJFile())
    }

}