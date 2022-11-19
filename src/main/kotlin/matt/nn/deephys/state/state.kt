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
	}
  }

  override fun serialize(value: DeephySettingsData) = jsonObj(
	"numImagesPerNeuronInByImage" to value.numImagesPerNeuronInByImage,
	"normalizeTopNeuronActivations" to value.normalizeTopNeuronActivations,
	"predictionSigFigs" to value.predictionSigFigs,
	"verboseLogging" to value.verboseLogging,
  )

}


@Serializable(DeephySettingsSerializer::class) class DeephySettingsData: SettingsData() {


  val numImagesPerNeuronInByImage by IntSettingProv(
	defaultValue = 9,
	label = "Number of images per neuron in top neurons row",
	tooltip = "Number of images per neuron in top neurons row",
	min = 9,
	max = 18
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
  val verboseLogging by BoolSettingProv(
	defaultValue = false,
	label = "Verbose Logging",
	tooltip = "Extra logging to standard out. May impact performance."
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

