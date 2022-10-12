package matt.nn.deephys.gui

import javafx.geometry.Pos.BOTTOM_LEFT
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import matt.file.construct.toMFile
import matt.file.toSFile
import matt.fx.graphics.hotkey.hotkeys
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.gui.app.GuiApp
import matt.lang.go
import matt.log.profile.stopwatch.Stopwatch
import matt.log.profile.stopwatch.tic
import matt.log.tab
import matt.model.latch.asyncloaded.LoadedValueSlot
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.deephyActionButton
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.settings.settingsButton
import matt.nn.deephys.init.modelBinding
import matt.nn.deephys.load.loadSwapper
import matt.nn.deephys.state.DeephyState
import matt.nn.deephys.version.VersionChecker

val stageTitle = LoadedValueSlot<String>()

fun startDeephyApp(t: Stopwatch? = null) = GuiApp(decorated = true) {


  val myStageTitle = stageTitle.await()
  stage.title = myStageTitle



  stage.node.minWidth = 1000.0
  stage.node.minHeight = 850.0
  /*stage.width = 1500.0
  stage.height = 1000.0*/



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

	hotkeys {
	  COMMA.meta {
		settingsButton.fire()
	  }
	}


	loadSwapper(modelBinding.await(), nullMessage = "Select a .model file to begin") {
	  val swapT = tic("model swapper", enabled = false)
	  swapT.toc(0)
	  val model = this@loadSwapper
	  VBoxWrapperImpl<NodeWrapper>().apply {

		swapT.toc(1)
		deephyText("Model: ${model.name}" + if (model.suffix != null) "_${model.suffix}" else "")
		swapT.toc(2)
		println("loaded model: ${model.name}")
		model.layers.forEach {
		  tab("${it.layerID} has ${it.neurons.size} neurons")
		}
		swapT.toc(3)

		val maxNeurons = 50
		val vis = if (model.layers.all { it.neurons.size <= maxNeurons }) {
		  ModelVisualizer(model).also {
			+it
			it.blue()
		  }
		} else {
		  deephyText("model is too large to visualize (>$maxNeurons in a layer)")
		  null
		}
		swapT.toc(4)
		val dSetViewsBox = DSetViewsVBox(model)
		swapT.toc(5)
		dSetViewsBox.modelVisualizer = vis
		vis?.dsetViewsBox = dSetViewsBox
		swapT.toc(6)
		val theTests = DeephyState.tests.value
		swapT.toc(6.5)
		theTests?.go {
		  dSetViewsBox += it
		}
		swapT.toc(7)
		+dSetViewsBox
		swapT.toc(8)
		deephyActionButton("add test") {
		  dSetViewsBox.addTest()
		}
		swapT.toc(9)
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


}.start(t = t)
