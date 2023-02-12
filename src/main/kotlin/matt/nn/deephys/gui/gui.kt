package matt.nn.deephys.gui

import javafx.geometry.Pos
import javafx.geometry.Pos.BOTTOM_LEFT
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.image.Image
import javafx.scene.layout.Priority.ALWAYS
import matt.async.thread.daemon
import matt.collect.itr.mapToArray
import matt.exec.app.myVersion
import matt.file.commons.LogContext
import matt.file.commons.PLATFORM_INDEPENDENT_APP_SUPPORT_FOLDER
import matt.fx.control.inter.graphic
import matt.fx.control.wrapper.scroll.scrollpane
import matt.fx.graphics.hotkey.hotkeys
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.node.parent.ParentWrapper
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.stage.StageWrapper
import matt.fx.node.proto.navDrawerButtonGraphic
import matt.gui.app.GuiApp
import matt.gui.app.warmup.warmupJvmThreading
import matt.gui.interact.WinOwn
import matt.gui.interact.openInNewWindow
import matt.gui.mscene.MScene
import matt.gui.mstage.ShowMode
import matt.image.ICON_SIZES
import matt.lang.anno.SeeURL
import matt.log.profile.stopwatch.Stopwatch
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.mstruct.rstruct.modID
import matt.mstruct.rstruct.resourceURL
import matt.nn.deephys.gui.Arg.`erase-settings`
import matt.nn.deephys.gui.Arg.`erase-state`
import matt.nn.deephys.gui.Arg.reset
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.deephyActionButton
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.navbox.NavBox
import matt.nn.deephys.gui.settings.DeephySettingsNode
import matt.nn.deephys.gui.settings.gui.settingsButton
import matt.nn.deephys.gui.visbox.VisBox
import matt.nn.deephys.init.initializeWhatICan
import matt.nn.deephys.init.warmupFxComponents
import matt.nn.deephys.state.DeephyState
import matt.nn.deephys.version.VersionChecker
import matt.obs.subscribe.Pager

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

	val settingsNode = DeephySettingsNode()

	if (args.size == 1 && args[0] == `erase-state`.name) {
	  DeephyState.delete()
	} else if (args.size == 1 && args[0] == `erase-settings`.name) {
	  settingsNode.delete()
	} else if (args.size == 1 && args[0] == reset.name) {
	  DeephyState.delete()
	  settingsNode.delete()
	} else {
	  warmupJvmThreading()

	  daemon {
		stageTitle.putLoadedValue("${modID.appName} $myVersion")
	  }

	  daemon {
		initializeWhatICan()
	  }

	  val lastVersion = DeephyState.lastVersionOpened.value!!
	  val thisVersion = modID.version.toString()
	  var openedNewVersion = false
	  if (lastVersion != thisVersion) {
		DeephyState.lastVersionOpened v thisVersion
		openedNewVersion = true
	  }

	  val settings = settingsNode.settings

	  val didSettingsReset = settings.wasResetBecauseSerializedDataWasWrongClassVersion

	  if (didSettingsReset) {


		settings.apply {
		  println("settings=$settings")
		  println("saving settings with new class version")
		  fakeSettingToForceLoading.value = -fakeSettingToForceLoading.value
		  println("saved with new class version")
		}


	  }

	  startDeephyApp(
		settingsNode = settingsNode,
		settingsDidReset = didSettingsReset,
		openedNewVersion = openedNewVersion
	  )


	}
  }

  val stageTitle = LoadedValueSlot<String>()

  val testReadyDSetViewsBbox = Pager<DSetViewsVBox>()
  val readyForConfiguringWindowFromTest = LoadedValueSlot<StageWrapper>()
  val testReadyScene = LoadedValueSlot<MScene<ParentWrapper<*>>>()


  fun startDeephyApp(
	t: Stopwatch? = null,
	settingsNode: DeephySettingsNode,
	settingsDidReset: Boolean,
	@Suppress("UNUSED_PARAMETER")
	openedNewVersion: Boolean
  ) = GuiApp(decorated = true) {


	warmupFxComponents(settingsNode.settings)


	val myStageTitle = stageTitle.await()
	stage.title = myStageTitle



	if (settingsDidReset) {
	  VBoxWrapperImpl<NW>().apply {
		deephysText("Welcome to Deephys")
		@Suppress("KotlinConstantConditions")
		if (settingsDidReset) {
		  deephysText("Your settings have been reset due to the new update.")
		}
		deephyActionButton("OK") {
		  this@deephyActionButton.stage!!.close()
		}
	  }.openInNewWindow(
		showMode = ShowMode.SHOW,
		own = WinOwn.Owner(stage),
		alwaysOnTop = true
	  )
	}


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

	  val settButton = settingsButton(settingsNode.settings).value

	  val navBox = NavBox().apply{
		visibleAndManaged = false
	  }

	  val visBox = VisBox(
		app = this@DeephysApp,
		settings = settingsNode.settings
	  )



	  hotkeys {
		COMMA.meta {
		  settButton.fire()
		}
	  }



	  h {
		deephyButton("") {
		  graphic = navDrawerButtonGraphic(prefHeight = settButton.heightProperty)
		  prefHeightProperty.bind(settButton.heightProperty)
		  setOnAction {
			navBox.visibleAndManaged = !navBox.visibleAndManaged
		  }
		}
		h {
		  hgrow = ALWAYS
		  alignment = Pos.CENTER_RIGHT
		  +settButton
		}
	  }

	  h {

		vgrow = ALWAYS

		fillHeightProperty.value = true

		+navBox

		scrollpane<VBoxWrapperImpl<NW>> {
		  hgrow = ALWAYS
		  hbarPolicy = NEVER
		  isFitToWidth = true

		  content = visBox
		}
	  }

/*
	  vbox<NodeWrapper> {
		vgrow = ALWAYS
	  }*/

	  vbox<NodeWrapper> {
		alignment = BOTTOM_LEFT
		+VersionChecker.statusNode
	  }
	}

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