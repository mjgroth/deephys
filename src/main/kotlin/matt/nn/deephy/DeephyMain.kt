@file:OptIn(ExperimentalSerializationApi::class)

package matt.nn.deephy

import javafx.geometry.Pos
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.DirectoryChooser
import kotlinx.serialization.ExperimentalSerializationApi
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.file.construct.toMFile
import matt.fx.graphics.lang.actionbutton
import matt.gui.app.GuiApp
import matt.hurricanefx.eye.bind.toStringConverter
import matt.hurricanefx.eye.collect.bind
import matt.hurricanefx.eye.collect.toObservable
import matt.hurricanefx.eye.lang.SProp
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.stringBinding
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.control.choice.ChoiceBoxWrapper
import matt.hurricanefx.wrapper.node.enableWhen
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.target.label
import matt.nn.deephy.model.DeephyDataManager
import matt.nn.deephy.model.DeephyDataManager.dataFile
import matt.nn.deephy.model.DeephyDataManager.dataFileTop
import matt.nn.deephy.model.DeephyDataManager.dataFolderProperty
import matt.nn.deephy.version.VersionChecker

fun main(): Unit = GuiApp(decorated = true) {

  stage.title = "$appName $myVersion"

  val statusProp = SProp("")

  VersionChecker.checkForUpdatesInBackground()

  val resultBox = VBoxWrapper()

  root<VBoxWrapper> {
	label(dataFolderProperty.stringBinding { "data folder: $it" })

	hbox {
	  actionbutton("choose data folder") {
		val f = DirectoryChooser().apply {
		  title = "choose data folder"
		}.showDialog(stage)

		if (f != null) {
		  dataFolderProperty.value = f.toMFile()
		}
	  }
	  button("load data") {
		enableWhen { dataFolderProperty.isNotNull }
		setOnAction {
		  statusProp.value = if (dataFileTop.value == null) "please select data folder"
		  else if (dataFileTop.value!!.doesNotExist) "${dataFileTop.value} does not exist"
		  else if (dataFile.value!!.doesNotExist) "${dataFile.value} does not exist"
		  else {
			val (top, image) = DeephyDataManager.load()
			resultBox.clear()
			resultBox.apply {
			  label("Layer ID: ${top.layerID}")
			  label("Layer Name: ${top.layerName}")
			  label("Num Neurons: ${top.numNeurons}")
			  var cb: ChoiceBoxWrapper<Int>? = null
			  hbox {
				label("choose neuron: ")
				cb = choicebox(values = (0 until top.numNeurons).toList().toObservable()) {
				  converter = toStringConverter { (it?.plus(1)).toString() }
				}
			  }
			  anchorpane {

			  }
			  vbox {
				children.bind(cb!!.valueProperty) {

				}
				cb!!.valueProperty.onChange {
				  clear()
				  if (it != null) {
					label(
					  "top images of unit ${it}: " + top.top100[it].joinToString(
						prefix = "[", postfix = "]", separator = ","
					  ),
					  wrap = true
					)
				  }
				}
			  }
			}
			"got data"
		  }
		}
	  }
	}

	+resultBox.apply {
	  vgrow = ALWAYS
	}
	vbox {
	  alignment = Pos.BOTTOM_LEFT
	  label(statusProp)
	  +VersionChecker.statusNode
	}
  }

}.start()


