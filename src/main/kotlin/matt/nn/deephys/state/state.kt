package matt.nn.deephys.state

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import matt.gui.option.SettingsData
import matt.json.custom.bool
import matt.json.custom.int
import matt.json.oldfx.jsonObj
import matt.json.ser.JsonObjectSerializer
import matt.lang.go
import matt.model.data.message.FileList
import matt.model.data.message.SFile
import matt.nn.deephys.calc.NormalizedAverageActivation.Companion.normalizeTopNeuronsBlurb
import matt.pref.obs.ObsPrefNode

object DeephyState: ObsPrefNode(
  "sinhalab.deephys.state",
  oldKeys = listOf(
	"dataFolder", "pref", "datasets"
  )
) {
  val model by obj<SFile>()
  val tests by obj<FileList>()
}

object DeephySettingsNode: ObsPrefNode(
  "sinhalab.deephys.settings",
  oldKeys = listOf(
	"numImagesPerNeuronInByImage", "normalizeTopNeuronActivations", "predictionSigFigs"
  )
) {
  val settings by obsObj { DeephySettingsData() }
}

object DeephySettingsSerializer: JsonObjectSerializer<DeephySettingsData>(DeephySettingsData::class) {
  override fun deserialize(jsonObject: JsonObject): DeephySettingsData {
	return DeephySettingsData().apply {
	  jsonObject["numImagesPerNeuronInByImage"]?.int?.go {
		numImagesPerNeuronInByImage.value = it
	  }
	  jsonObject["normalizeTopNeuronActivations"]?.bool?.go {
		normalizeTopNeuronActivations.value = it
	  }
	  jsonObject["predictionSigFigs"]?.int?.go {
		predictionSigFigs.value = it
	  }
	  jsonObject["verboseLogging"]?.bool?.go {
		verboseLogging.value = it
	  }
	  jsonObject["millisecondsBeforeTooltipsVanish"]?.int?.go {
		millisecondsBeforeTooltipsVanish.value = it
	  }
	}
  }

  override fun serialize(value: DeephySettingsData) = jsonObj(
	*value.namedObservables().map {
	  it.key to it.value
	}.toTypedArray()
  )

}

const val MAX_NUM_IMAGES_IN_TOP_NEURONS = 18


@Serializable(DeephySettingsSerializer::class) class DeephySettingsData: SettingsData() {


  val smallImageScale by DoubleSettingProv(
	defaultValue = 32.0,
	label = "Small image scale",
	tooltip = "the width (in pixels) for default images",
	min = 10.0,
	max = 100.0
  )
  val bigImageScale by DoubleSettingProv(
	defaultValue = 128.0,
	label = "Big image scale",
	tooltip = "the width (in pixels) for big images",
	min = 110.0,
	max = 200.0
  )
  val normalizeTopNeuronActivations by BoolSettingProv(
	defaultValue = false,
	label = "Normalize activations of top neurons",
	tooltip = normalizeTopNeuronsBlurb
  )
  val predictionSigFigs by IntSettingProv(
	defaultValue = 5,
	label = "Prediction value significant figures",
	tooltip = "Prediction value significant figures",
	min = 3,
	max = 10
  )
  val numImagesPerNeuronInByImage by IntSettingProv(
	defaultValue = 9,
	label = "Number of images per neuron in top neurons row",
	tooltip = "Number of images per neuron in top neurons row",
	min = 9,
	max = MAX_NUM_IMAGES_IN_TOP_NEURONS
  )
  val millisecondsBeforeTooltipsVanish by IntSettingProv(
	defaultValue = 5000,
	label = "tooltip hide delay (ms)",
	tooltip = "Milliseconds before tooltips vanish. 0 means infinite (hit ESCAPE to make them go away)",
	min = 0,
	max = 10000
  )
  val verboseLogging by BoolSettingProv(
	defaultValue = false,
	label = "Verbose Logging",
	tooltip = "Extra logging to standard out. May impact performance."
  )
  val showCacheBars by BoolSettingProv(
	defaultValue = false,
	label = "Cache Progress Bars",
	tooltip = "Extra progress bars indicating the progress of data caching."
  )
  val showTutorials by BoolSettingProv(
	defaultValue = true,
	label = "Show Tutorials",
	tooltip = "Show Interactive Tutorials Throughout the app"
  )

  init {
	observe {
	  println("big obj changed")
	}
  }

}

/*Todo: I wish this could be an object*/
val DeephySettings by lazy {
  DeephySettingsNode.settings
}

