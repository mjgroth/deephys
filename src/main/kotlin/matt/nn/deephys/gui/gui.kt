package matt.nn.deephys.gui

import javafx.application.Platform.runLater
import javafx.geometry.Pos.BOTTOM_LEFT
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.image.Image
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import matt.async.thread.daemon
import matt.auto.ICON_SIZES
import matt.collect.itr.mapToArray
import matt.exec.app.myVersion
import matt.file.commons.LogContext
import matt.file.commons.PLATFORM_INDEPENDENT_APP_SUPPORT_FOLDER
import matt.file.construct.toMFile
import matt.file.toSFile
import matt.fx.control.mscene.MScene
import matt.fx.control.wrapper.scroll.scrollpane
import matt.fx.graphics.hotkey.hotkeys
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.node.parent.ParentWrapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapper
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.stage.StageWrapper
import matt.gui.app.GuiApp
import matt.gui.app.warmup.warmupJvmThreading
import matt.lang.anno.SeeURL
import matt.lang.go
import matt.log.profile.stopwatch.Stopwatch
import matt.log.profile.stopwatch.tic
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.mstruct.rstruct.appName
import matt.mstruct.rstruct.resourceURL
import matt.nn.deephys.gui.Arg.`erase-settings`
import matt.nn.deephys.gui.Arg.`erase-state`
import matt.nn.deephys.gui.Arg.reset
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.deephyActionButton
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.settings.settingsButton
import matt.nn.deephys.init.initializeWhatICan
import matt.nn.deephys.init.modelBinding
import matt.nn.deephys.load.loadSwapper
import matt.nn.deephys.state.DeephySettingsNode
import matt.nn.deephys.state.DeephyState
import matt.nn.deephys.version.VersionChecker
import matt.obs.subscribe.Pager
import java.util.prefs.Preferences

val DEEPHY_USER_DATA_DIR by lazy {
  PLATFORM_INDEPENDENT_APP_SUPPORT_FOLDER.mkdir("Deephys")
}
val DEEPHYS_LOG_CONTEXT by lazy {
  LogContext(DEEPHY_USER_DATA_DIR)
}

enum class Arg {
  `erase-state`, `erase-settings`, reset
}

class DeephysApp {

  fun boot(vararg args: Arg): Unit = boot(args.mapToArray { it.name })

  /*invoked directly from test, in case I ever want to return something*/
  fun boot(args: Array<String>) {
	if (args.size == 1 && args[0] == `erase-state`.name) {
	  DeephyState.delete()
	} else if (args.size == 1 && args[0] == `erase-settings`.name) {
	  DeephySettingsNode.delete()
	} else if (args.size == 1 && args[0] == reset.name) {
	  DeephyState.delete()
	  DeephySettingsNode.delete()
	} else {
	  warmupJvmThreading()

	  daemon {
		stageTitle.putLoadedValue("$appName $myVersion")
	  }

	  daemon {
		initializeWhatICan()
	  }

	  daemon {
		Preferences.userRoot().node("sinhalab.deephy.state").apply {
		  removeNode()
		  flush()
		}
		Preferences.userRoot().node("sinhalab.deephy.settings").apply {
		  removeNode()
		  flush()
		}
	  }

	  startDeephyApp()


	}
  }

  val stageTitle = LoadedValueSlot<String>()

  val testReadyDSetViewsBbox = Pager<DSetViewsVBox>()
  val readyForConfiguringWindowFromTest = LoadedValueSlot<StageWrapper>()
  val testReadyScene = LoadedValueSlot<MScene<ParentWrapper<*>>>()


  fun startDeephyApp(t: Stopwatch? = null) = GuiApp(decorated = true) {

	//	println("start app 1")

	val myStageTitle = stageTitle.await()
	stage.title = myStageTitle



	stage.icons.addAll(
	  ICON_SIZES.map {
		Image(resourceURL("logo_$it.png").toString())
	  }
	)

	//	println("start app 2")

	stage.node.minWidth = 1000.0
	@SeeURL("https://www.theverge.com/2013/7/15/4523668/11-inch-macbook-air-review")
	stage.node.minHeight = 750.0
	/*stage.width = 1500.0
  stage.height = 1000.0*/

	readyForConfiguringWindowFromTest.putLoadedValue(stage)

	//	println("start app 3")

	root<VBoxWrapperImpl<NodeWrapper>> {


	  alignment = TOP_CENTER

	  hotkeys {
		COMMA.meta {
		  settingsButton.fire()
		}
	  }

	  scrollpane<VBoxWrapper<NW>>() {
		hbarPolicy = NEVER
		isFitToWidth = true





		content = VBoxWrapperImpl<NodeWrapper>().apply {
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


		  loadSwapper(modelBinding.await(), nullMessage = "Select a .model file to begin") {
			val swapT = tic("model swapper", enabled = true)
			swapT.toc(0)
			val model = this@loadSwapper
			VBoxWrapperImpl<NodeWrapper>().apply {

			  swapT.toc(1)
			  deephyText("Model: ${model.name}" + if (model.suffix != null) "_${model.suffix}" else "")
			  swapT.toc(2)
			  println("loaded model: ${model.name}")
			  println(model.infoString())
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
			  runLater {
				testReadyDSetViewsBbox.page(dSetViewsBox)
			  }
			}
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
	println("start app 4")

	/*not currently using this, because after making scroll bars transparet I found out that nothing was in fact being laid out underneath them, so it was just creating a weird space. Search for search key FRHWOIH83RH3URUG34TGOG34G934G */
	/*  scene!!.stylesheets.add(ClassLoader.getSystemResource("deephys.css").toString())*/

	testReadyScene.putLoadedValue(scene!!)

	println("put loaded scene")

	VersionChecker.checkForUpdatesInBackground()

  }.runBlocking(
	logContext = DEEPHYS_LOG_CONTEXT,
	t = t
  )

}