package matt.nn.deephys.gui

import javafx.application.Platform.runLater
import javafx.geometry.Pos
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
import matt.fx.graphics.wrapper.node.disableWhen
import matt.fx.graphics.wrapper.node.parent.ParentWrapper
import matt.fx.graphics.wrapper.node.visibleAndManagedWhen
import matt.fx.graphics.wrapper.pane.anchor.swapper.swap
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapper
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.stage.StageWrapper
import matt.gui.app.GuiApp
import matt.gui.app.warmup.warmupJvmThreading
import matt.hurricanefx.eye.prop.sizeProperty
import matt.lang.anno.SeeURL
import matt.lang.go
import matt.log.profile.stopwatch.Stopwatch
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.mstruct.rstruct.appName
import matt.mstruct.rstruct.resourceURL
import matt.nn.deephys.gui.Arg.`erase-settings`
import matt.nn.deephys.gui.Arg.`erase-state`
import matt.nn.deephys.gui.Arg.reset
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.DEEPHYS_FONT_MONO
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyCheckbox
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.global.tooltip.deephysInfoSymbol
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.settings.DeephySettingsNode
import matt.nn.deephys.init.initializeWhatICan
import matt.nn.deephys.init.modelBinding
import matt.nn.deephys.init.warmupFxComponents
import matt.nn.deephys.load.loadSwapper
import matt.nn.deephys.state.DeephyState
import matt.nn.deephys.version.VersionChecker
import matt.obs.bind.binding
import matt.obs.prop.BindableProperty
import matt.obs.subscribe.Pager
import matt.prim.str.mybuild.string
import matt.prim.str.truncateWithElipsesOrAddSpaces
import matt.nn.deephys.gui.settings.gui.settingsButton
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


	warmupFxComponents()


	val myStageTitle = stageTitle.await()
	stage.title = myStageTitle



	stage.icons.addAll(
	  ICON_SIZES.map {
		Image(resourceURL("logo_$it.png").toString())
	  }
	)


	stage.node.minWidth = 1000.0
	@SeeURL("https://www.theverge.com/2013/7/15/4523668/11-inch-macbook-air-review")
	stage.node.minHeight = 750.0
	/*stage.width = 1500.0
  stage.height = 1000.0*/

	readyForConfiguringWindowFromTest.putLoadedValue(stage)


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





		content = VBoxW().apply {

		  spacing = 10.0
		  alignment = TOP_CENTER

		  val visualizer = BindableProperty<ModelVisualizer?>(null)
		  val pleaseLoadModelToSeeVisualizerText = "Choose a model in order to visualize it"
		  val visualizerToolTipText = BindableProperty(pleaseLoadModelToSeeVisualizerText)
		  val showVisualizer = BindableProperty(false)

		  h {
			spacing = 25.0

			deephyButton("Choose Model") {
			  prefHeightProperty.bind(settingsButton.heightProperty)
			  setOnAction {
				val f = FileChooser().apply {
				  extensionFilters.setAll(ExtensionFilter("model files", "*.model"))
				}.showOpenDialog(stage?.node)?.toMFile()?.toSFile()
				if (f != null) {
				  DeephyState.tests.value = null
				  DeephyState.model.value = f
				}
			  }
			}

			h {
			  deephyTooltip(visualizerToolTipText) /*matt.fx.control.wrapper.tooltip.fixed.tooltip has to be outside of checkbox or else it will not show when checkbox is disabled?*/
			  deephyCheckbox("Show Model Diagram") {
				prefHeightProperty.bind(settingsButton.heightProperty)
				visualizer.onChange {
				  if (it == null) {
					isSelected = false
				  }
				}
				disableWhen { visualizer.isNull }

				showVisualizer.bind(selectedProperty)
			  }
			}

			h {
			  hgrow = ALWAYS
			  alignment = Pos.CENTER_RIGHT
			  +settingsButton
			}

		  }



		  v {
			visibleAndManagedWhen { showVisualizer }
			swap(visualizer)
		  }


		  loadSwapper(modelBinding.await(), nullMessage = "Select a .model file to begin") {
			val model = this@loadSwapper
			VBoxWrapperImpl<NodeWrapper>().apply {

			  h {
				spacing = 10.0
				deephyText("Model: ${model.name}" + if (model.suffix != null) "_${model.suffix}" else "") {
				  titleFont()
				}
				deephysInfoSymbol(
				  string {
					lineDelimited {
					  +"Layers"
					  model.layers.forEach {
						+"\t${it.layerID.truncateWithElipsesOrAddSpaces(15)}: ${it.neurons.size}"
					  }
					}
				  }
				) {
				  fontProperty v DEEPHYS_FONT_MONO
				  /*wrapTextProp v true*/
				}
			  }


			  println("loaded model: " + model.infoString())

			  val maxNeurons = 50


			  val vis = if (model.layers.all { it.neurons.size <= maxNeurons }) {
				visualizerToolTipText v "Show an interactive diagram of the model"
				ModelVisualizer(model)
			  } else {
				visualizerToolTipText v "model is too large to visualize (>$maxNeurons in a layer)"
				null
			  }
			  visualizer v vis

			  val dSetViewsBox = DSetViewsVBox(model)
			  dSetViewsBox.modelVisualizer = vis
			  vis?.dsetViewsBox = dSetViewsBox
			  val theTests = DeephyState.tests.value
			  theTests?.go {
				dSetViewsBox += it
			  }
			  +dSetViewsBox
			  deephyButton("Add a test") {
				textProperty.bind(dSetViewsBox.children.sizeProperty.binding {
				  if (it == 0) "Add a test" else "+"
				})
				deephyTooltip("Add a test")
				setOnAction {
				  dSetViewsBox.addTest()
				}

			  }
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