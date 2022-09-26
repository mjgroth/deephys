package matt.nn.deephy

import javafx.geometry.Pos
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import matt.async.thread.daemon
import matt.auto.myPid
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.file.CborFile
import matt.file.construct.toMFile
import matt.file.toMFile
import matt.file.toSFile
import matt.gui.app.GuiApp
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.log.profile.MemReport
import matt.log.taball
import matt.nn.deephy.gui.DSetViewsVBox
import matt.nn.deephy.gui.global.deephyActionButton
import matt.nn.deephy.gui.global.deephyText
import matt.nn.deephy.gui.settings.settingsButton
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.load.loadCbor
import matt.nn.deephy.load.loadSwapper
import matt.nn.deephy.model.Model
import matt.nn.deephy.state.DeephyState
import matt.nn.deephy.version.VersionChecker
import matt.obs.bind.binding
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.minutes


fun main(): Unit = GuiApp(decorated = true) {


  println("my pid = $myPid")
  daemon {
	while (true) {
	  println(MemReport())
	  sleep(1.minutes.inWholeMilliseconds)
	}
  }



  stage.title = "$appName $myVersion"
  stage.node.minWidth = 1000.0
  stage.node.minHeight = 850.0
  stage.width = 1000.0
  stage.height = 850.0

  VersionChecker.checkForUpdatesInBackground()

  root<VBoxWrapper<NodeWrapper>> {

	alignment = Pos.TOP_CENTER

	hbox<NodeWrapper> {
	  deephyActionButton("choose model file") {
		val f = FileChooser().apply {
		  extensionFilters.setAll(ExtensionFilter("model files", "*.model"))
		}.showOpenDialog(stage)?.toMFile()?.toSFile()
		if (f != null) {
		  DeephyState.tests.value = null
		  DeephyState.model.value = f
		}
	  }

	  +settingsButton

	}

	val modelDataBinding = DeephyState.model.binding { f ->
	  f?.toMFile()?.loadCbor<Model>()
	}


	loadSwapper(modelDataBinding, nullMessage = "Select a .model file to begin") {

	  val model = this@loadSwapper
	  VBoxWrapper<NodeWrapper>().apply {
		deephyText("Model: ${model.name}" + if (model.suffix != null) "_${model.suffix}" else "")
		val multiAcc = DSetViewsVBox(model).apply {
		  DeephyState.tests.value?.forEach {
			this += (CborFile(it.path))
		  }
		}
		+multiAcc
		deephyActionButton("add dataset") {
		  multiAcc += DatasetViewer(null, multiAcc)
		  taball("children of multAcc:", multiAcc.node.children)
		}
	  }
	}

	vbox<NodeWrapper> {
	  vgrow = ALWAYS
	}

	vbox<NodeWrapper> {
	  alignment = Pos.BOTTOM_LEFT
	  +VersionChecker.statusNode
	}
  }

}.start()


