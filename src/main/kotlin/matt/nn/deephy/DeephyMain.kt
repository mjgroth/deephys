@file:OptIn(ExperimentalSerializationApi::class)

package matt.nn.deephy

import javafx.stage.DirectoryChooser
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import matt.exec.app.appName
import matt.file.MFile
import matt.file.construct.mFile
import matt.file.construct.toMFile
import matt.fx.graphics.lang.actionbutton
import matt.gui.app.GuiApp
import matt.hurricanefx.eye.collect.toObservable
import matt.hurricanefx.eye.lang.Prop
import matt.hurricanefx.eye.lang.SProp
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.objectBinding
import matt.hurricanefx.eye.prop.stringBinding
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.control.choice.ChoiceBoxWrapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.target.label
import matt.nn.deephy.model.DeephyData
import matt.nn.deephy.model.ImageV2
import matt.nn.deephy.model.TopV2
import matt.nn.deephy.pref.Pref
import matt.nn.deephy.version.VersionChecker


fun main() = GuiApp(decorated = true) {

  stage.title = appName

  val statusProp = SProp("")

  VersionChecker.checkForUpdatesInBackground()


  var dataFolder by Pref()
  val dataFolderProperty = Prop<MFile>()
  dataFolder?.let { dataFolderProperty.value = mFile(it) }
  dataFolderProperty.onChange {
	dataFolder = it?.abspath
  }

  val dataFile = dataFolderProperty.objectBinding {
	it?.resolve("CIFAR10_test.cbor")
  }
  val dataFileV2 = dataFolderProperty.objectBinding {
	it?.resolve("CIFAR10_test_v2.cbor")
  }
  val dataFileTop = dataFolderProperty.objectBinding {
	it?.resolve("CIFAR10_test_top.cbor")
  }
  val dataFileTopV2 = dataFolderProperty.objectBinding {
	it?.resolve("CIFAR10_test_top_v2.cbor")
  }

  val deephyDataFile = dataFolderProperty.objectBinding {
	it?.resolve("deephyData0.cbor")
  }


  val resultBox = VBoxWrapper()

  root<VBoxWrapper> {
	label(dataFolderProperty.stringBinding {
	  "data folder: $it"
	})
	actionbutton("choose data folder") {
	  val f = DirectoryChooser().apply {
		title = "choose data folder"
	  }.showDialog(stage)

	  if (f != null) {
		dataFolderProperty.value = f.toMFile()
	  }
	}
	actionbutton("load data") {
	  if (dataFileTop.value == null) {
		statusProp.value = "please select data folder"
	  } else if (dataFileTop.value!!.doesNotExist) {
		statusProp.value = "${dataFileTop.value} does not exist"
	  } else if (dataFile.value!!.doesNotExist) {
		statusProp.value = "${dataFile.value} does not exist"
	  } else {

		//		val top = Cbor.decodeFromByteArray<Top>(dataFileTop.value!!.readBytes())
		//		val data = Cbor.decodeFromByteArray<Image>(dataFile.value!!.readBytes())


		val top = Cbor.decodeFromByteArray<TopV2>(dataFileTopV2.value!!.readBytes())
		val data = Cbor.decodeFromByteArray<ImageV2>(dataFileV2.value!!.readBytes())


		val deephyData = Cbor.decodeFromByteArray<DeephyData>(deephyDataFile.value!!.readBytes())


		resultBox.clear()
		resultBox.apply {
		  label("Layer ID: ${top.layerID}")
		  label("Layer Name: ${top.layerName}")
		  label("Num Neurons: ${top.numNeurons}")
		  var cb: ChoiceBoxWrapper<Int>? = null
		  hbox {
			label("choose neuron: ")
			cb = choicebox(values = (0..top.numNeurons).toList().toObservable())
		  }
		  vbox {
			cb!!.valueProperty.onChange {
			  clear()
			  if (it != null) {
				label(
				  "top images of unit ${it}: " + top.top100[it].joinToString(
					prefix = "[", postfix = "]", separator = ","
				  )
				) {
				  isWrapText = true
				}
			  }
			}
		  }
		}
		statusProp.value = "got data"
	  }
	}
	label(statusProp)
	+resultBox
	+VersionChecker.statusNode
  }

}.start()


