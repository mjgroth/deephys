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
import matt.nn.deephy.model.Top
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
  val dataFileTop = dataFolderProperty.objectBinding {
	it?.resolve("CIFAR10_test_top.cbor")
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
		val top = Cbor.decodeFromByteArray<Top>(dataFileTop.value!!.readBytes())
		/*val data = Cbor.decodeFromByteArray<Array<matt.nn.deephy.model.Image>>(dataFileTop.value!!.readBytes())*/
		resultBox.clear()
		resultBox.apply {
		  label("Layer ID: ${top.layer_ID}")
		  label("Layer Name: ${top.layer_Name}")
		  label("Num Neurons: ${top.num_Neurons}")
		  var cb: ChoiceBoxWrapper<Int>? = null
		  hbox {
			label("choose neuron: ")
			cb = choicebox(values = (0..top.num_Neurons).toList().toObservable())
		  }
		  vbox {
			cb!!.valueProperty.onChange {
			  clear()
			  if (it != null) {
				label(
				  "top images of unit ${it}: " + top.top_100[it].joinToString(
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


