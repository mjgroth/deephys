package matt.nn.deephy.state

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import matt.hurricanefx.eye.pref.FXPrefNode
import matt.json.custom.bool
import matt.json.custom.int
import matt.json.fx.jsonObj
import matt.json.ser.JsonObjectSerializer
import matt.lang.go
import matt.nn.deephy.calc.NormalizedActivation.Companion.normalizeTopNeuronsBlurb
import matt.obs.hold.ObservableHolderImpl
import matt.obs.prop.Var
import matt.stream.message.FileList
import matt.stream.message.SFile
import kotlin.reflect.KProperty

object DeephyState: FXPrefNode(
  "sinhalab.deephy.state", oldKeys = listOf(
	"dataFolder", "pref", "datasets"
  )
) {
  val model by obj<SFile>()
  val tests by obj<FileList>()

}

private object DeephySettingsNode: FXPrefNode(
  "sinhalab.deephy.settings", oldKeys = listOf(
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
	}
  }

  override fun serialize(value: DeephySettingsData) = jsonObj(
	"numImagesPerNeuronInByImage" to value.numImagesPerNeuronInByImage,
	"normalizeTopNeuronActivations" to value.normalizeTopNeuronActivations,
	"predictionSigFigs" to value.predictionSigFigs
  )

}

sealed class Setting<T>(val prop: Var<T>, val label: String, val tooltip: String) {
  init {
	prop.observe {
	  println("Setting.prop changed...")
	}
  }
}
class IntSetting(prop: Var<Int>, label: String, tooltip: String, val min: Int, val max: Int):
  Setting<Int>(prop, label = label, tooltip = tooltip)

class BoolSetting(prop: Var<Boolean>, label: String, tooltip: String):
  Setting<Boolean>(prop, label = label, tooltip = tooltip)


@Serializable(DeephySettingsSerializer::class) class DeephySettingsData: ObservableHolderImpl() {
  private val mSettings = mutableListOf<Setting<*>>()
  val settings: List<Setting<*>> = mSettings



  private inner class BoolSettingProv(
	private val defaultValue: Boolean,
	private val label: String,
	private val tooltip: String
  ) {
	operator fun provideDelegate(
	  thisRef: ObservableHolderImpl,
	  prop: KProperty<*>,
	) = RegisteredProp(defaultValue).provideDelegate(thisRef, prop).also {
	  mSettings += BoolSetting(it.getValue(thisRef, prop), label = label, tooltip = tooltip)
	}
  }

  private inner class IntSettingProv(
	private val defaultValue: Int,
	private val label: String,
	private val tooltip: String,
	private val min: Int,
	private val max: Int
  ) {
	operator fun provideDelegate(
	  thisRef: ObservableHolderImpl,
	  prop: KProperty<*>,
	) = RegisteredProp(defaultValue).provideDelegate(thisRef, prop).also {
	  mSettings += IntSetting(it.getValue(thisRef, prop), label = label, tooltip = tooltip, min = min, max = max)
	}
  }

  val numImagesPerNeuronInByImage by IntSettingProv(
	defaultValue = 9,
	label = "Number of images per neuron in image view",
	tooltip = "Number of images per neuron in image view",
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


  init {
	observe {
	  println("biig obj changed")
	}
  }

}

/*Todo: I wish this could be an object*/
val DeephySettings by lazy {
  DeephySettingsNode.settings
}