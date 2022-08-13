@file:OptIn(ExperimentalSerializationApi::class)

package matt.nn.deephy

import javafx.geometry.Pos
import javafx.scene.layout.Priority.ALWAYS
import kotlinx.serialization.ExperimentalSerializationApi
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.file.CborFile
import matt.file.toMFile
import matt.fx.graphics.lang.actionbutton
import matt.gui.app.GuiApp
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.nn.deephy.gui.DatasetViewer
import matt.nn.deephy.state.DeephyState
import matt.nn.deephy.version.VersionChecker

fun main(): Unit = GuiApp(decorated = true) {

  stage.title = "$appName $myVersion"
  stage.node.minWidth = 600.0
  stage.node.minHeight = 850.0
  stage.width = 600.0
  stage.height = 850.0

  VersionChecker.checkForUpdatesInBackground()

  root<VBoxWrapper> {

	alignment = Pos.TOP_CENTER

	val multiAcc = vbox {
	  DeephyState.datasets.value?.forEach {
		+DatasetViewer(it.toMFile() as CborFile)
	  }
	}
	actionbutton("add dataset") {
	  multiAcc += DatasetViewer()
	}

	vbox {
	  vgrow = ALWAYS
	}

	vbox {
	  alignment = Pos.BOTTOM_LEFT
	  +VersionChecker.statusNode
	}
  }

}.start()


