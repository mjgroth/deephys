package matt.nn.deephy.gui

import javafx.geometry.Pos.BOTTOM_LEFT
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.file.construct.toMFile
import matt.file.toMFile
import matt.file.toSFile
import matt.gui.app.GuiApp
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.lang.go
import matt.nn.deephy.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephy.gui.global.deephyActionButton
import matt.nn.deephy.gui.global.deephyText
import matt.nn.deephy.gui.modelvis.ModelVisualizer
import matt.nn.deephy.gui.settings.settingsButton
import matt.nn.deephy.load.loadCbor
import matt.nn.deephy.load.loadSwapper
import matt.nn.deephy.model.importformat.Model
import matt.nn.deephy.state.DeephyState
import matt.nn.deephy.version.VersionChecker
import matt.obs.bind.binding

fun startDeephyApp() = GuiApp(decorated = true) {

  stage.title = "$appName $myVersion"
  stage.node.minWidth = 1000.0
  stage.node.minHeight = 850.0
  stage.width = 1000.0
  stage.height = 850.0


  root<VBoxWrapperImpl<NodeWrapper>> {
	alignment = TOP_CENTER

	hbox<NodeWrapper> {
	  deephyActionButton("choose model file") {
		val f = FileChooser().apply {
		  extensionFilters.setAll(ExtensionFilter("model files", "*.model"))
		}.showOpenDialog(stage?.node)?.toMFile()?.toSFile()
		if (f != null) {
		  DeephyState.tests.value = null
		  DeephyState.model.value = f
		}
	  }

	  +settingsButton

	}

	loadSwapper(DeephyState.model.binding { f ->
	  f?.toMFile()?.loadCbor<Model>()
	}, nullMessage = "Select a .model file to begin") {
	  val model = this@loadSwapper
	  VBoxWrapperImpl<NodeWrapper>().apply {

		val vis = ModelVisualizer(model)
		+vis
		deephyText("Model: ${model.name}" + if (model.suffix != null) "_${model.suffix}" else "")
		val dSetViewsBox = DSetViewsVBox(model)

		dSetViewsBox.modelVisualizer = vis
		vis.dsetViewsBox = dSetViewsBox


		DeephyState.tests.value?.go {
		  dSetViewsBox += it
		}
		+dSetViewsBox
		deephyActionButton("add test") {
		  dSetViewsBox.addTest()
		}
	  }
	}

	vbox<NodeWrapper> {
	  vgrow = ALWAYS
	}

	vbox<NodeWrapper> {
	  alignment = BOTTOM_LEFT
	  +VersionChecker.statusNode
	}
  }

}.start()
