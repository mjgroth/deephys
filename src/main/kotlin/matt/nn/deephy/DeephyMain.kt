@file:OptIn(ExperimentalSerializationApi::class)

package matt.nn.deephy

import javafx.geometry.Pos
import javafx.scene.layout.Priority.ALWAYS
import kotlinx.serialization.ExperimentalSerializationApi
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.fx.graphics.lang.actionbutton
import matt.gui.app.GuiApp
import matt.hurricanefx.eye.lang.SProp
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.target.label
import matt.nn.deephy.gui.DatasetViewer
import matt.nn.deephy.version.VersionChecker

fun main(): Unit = GuiApp(decorated = true) {

  stage.title = "$appName $myVersion"
  stage.node.minWidth = 600.0
  stage.node.minHeight = 600.0
  stage.width = 600.0
  stage.height = 600.0


  val statusProp = SProp("")

  VersionChecker.checkForUpdatesInBackground()

  //  val resultBox = VBoxWrapper()

  root<VBoxWrapper> {

	alignment = Pos.TOP_CENTER

	//	+dataFolderNode

	val acc = accordion {

	}
	actionbutton("add dataset") {
	  acc.panes += DatasetViewer(container = acc.panes).node
	}

	vbox {
	  vgrow = ALWAYS
	}

	vbox {
	  alignment = Pos.BOTTOM_LEFT
	  label(statusProp)
	  +VersionChecker.statusNode
	}
  }

}.start()


