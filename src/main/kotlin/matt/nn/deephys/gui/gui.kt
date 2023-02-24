package matt.nn.deephys.gui

import javafx.geometry.Pos
import javafx.geometry.Pos.BOTTOM_LEFT
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.image.Image
import javafx.scene.layout.Priority.ALWAYS
import matt.async.pool.DaemonPool
import matt.async.thread.daemon
import matt.collect.itr.mapToArray
import matt.exec.app.myVersion
import matt.file.MFile
import matt.file.commons.LogContext
import matt.file.commons.PLATFORM_INDEPENDENT_APP_SUPPORT_FOLDER
import matt.fx.control.inter.graphic
import matt.fx.control.mail
import matt.fx.control.wrapper.progressbar.progressbar
import matt.fx.control.wrapper.scroll.scrollpane
import matt.fx.graphics.fxthread.ts.nonBlockingFXWatcher
import matt.fx.graphics.hotkey.hotkeys
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.node.parent.ParentWrapper
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.stage.StageWrapper
import matt.fx.node.proto.navDrawerButtonGraphic
import matt.gui.app.GuiApp
import matt.gui.app.warmup.warmupJvmThreading
import matt.gui.exception.openNewYouTrackIssue
import matt.gui.interact.WinOwn
import matt.gui.interact.openInNewWindow
import matt.gui.interact.popupWarning
import matt.gui.mscene.MScene
import matt.gui.mstage.ShowMode
import matt.gui.mstage.ShowMode.SHOW_AND_WAIT
import matt.gui.mstage.WMode.NOTHING
import matt.http.internet.TheInternet
import matt.http.internet.isAvailable
import matt.image.ICON_SIZES
import matt.lang.anno.SeeURL
import matt.lang.err
import matt.lang.sync
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
import matt.nn.deephys.gui.global.deephysLabel
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.navbox.NavBox
import matt.nn.deephys.gui.navbox.ZooExample
import matt.nn.deephys.gui.settings.DeephySettingsNode
import matt.nn.deephys.gui.settings.gui.SettingsWindow
import matt.nn.deephys.gui.visbox.VisBox
import matt.nn.deephys.init.initializeWhatICan
import matt.nn.deephys.init.warmupFxComponents
import matt.nn.deephys.state.DeephyState
import matt.nn.deephys.version.VersionChecker
import matt.obs.prop.BindableProperty
import matt.obs.subscribe.Pager
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

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

  fun boot2(settingsNode: DeephySettingsNode, vararg args: Arg): Unit =
	boot(args.mapToArray { it.name }, settingsNode = settingsNode)

  /*invoked directly from test, in case I ever want to return something*/
  fun boot(
	args: Array<String>,
	settingsNode: DeephySettingsNode = DeephySettingsNode()
  ) {


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

  var visBox: VisBox? = null
  var navBox: NavBox? = null
  fun showDemos() {
	navBox!!.visibleAndManaged = true
	navBox!!.showDemos()
  }

  fun openZooDemo(demo: ZooExample) {

	if (!TheInternet().isAvailable()) {
	  popupWarning("No internet connection")
	  return
	}

	val pool = DaemonPool()

	val modelURL = URI(demo.modelURL).toURL()
	val testURLs = demo.testURLs.map { URI(it).toURL() }


	val total = testURLs.size + 1
	val done = AtomicInteger(0)
	val progress = BindableProperty(0.0)

	val monitor = object {}

	val modelFile = pool.submit {
	  val f = MFile.Companion.createTempFile("model_${demo.name}", suffix = "")
	  modelURL.openStream().use { downloadStream ->
		f.outputStream().use { writeStream ->
		  downloadStream.transferTo(writeStream)
		}
	  }
	  done.incrementAndGet()
	  monitor.sync {
		progress v done.get().toDouble()/total
	  }
	  f
	}

	val testFiles = testURLs.mapIndexed { i, testURL ->
	  pool.submit {
		val f = MFile.Companion.createTempFile("test_${i}", suffix = "")
		testURL.openStream().use { downloadStream ->
		  f.outputStream().use { writeStream ->
			downloadStream.transferTo(writeStream)
		  }
		}
		done.incrementAndGet()
		monitor.sync {
		  progress v done.get().toDouble()/total
		}
		f
	  }
	}




	VBoxW().apply {


	  deephysLabel("Downloading ${demo.name}...")

	  val prog = progressbar {

	  }

	  deephysLabel("Loading Files... (0/${total})") {
		progress.nonBlockingFXWatcher().onChange {
		  prog.progress = it
		  text = "Loading Files... (${done.get()}/${total})"
		  if (done.get() == total) {
			stage!!.close()
			navBox!!.visibleAndManaged = false
			val theVisBox = visBox ?: err("no visBox!")
			theVisBox.load(
			  modelFile = modelFile.get(),
			  testFiles = testFiles.map { it.get() }
			)
		  }
		}
	  }


	}.openInNewWindow(
	  showMode = SHOW_AND_WAIT,
	  wMode = NOTHING,
	  alwaysOnTop = true
	) {

	}

	modelURL.openStream()

	/*root.findRecursivelyFirstOrNull<DSetViewsVBox>()?.removeAllTests()*/
  }

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

	  val settButton = SettingsWindow(settingsNode.settings).button(this)

	  navBox = NavBox(this@DeephysApp).apply {
		visibleAndManaged = false
	  }


	  visBox = VisBox(
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
			navBox!!.visibleAndManaged = !navBox!!.visibleAndManaged
		  }
		}
		h {
		  hgrow = ALWAYS
		  alignment = Pos.CENTER_RIGHT
		  /*spacing = DEEPHYS_SYMBOL_SPACING*/
		  deephyActionButton ("Report Bug") {
			openNewYouTrackIssue(
			  summary = "Bug Report",
			  description = ""
			)
		  }.apply {
			prefHeightProperty.bind(settButton.heightProperty)
		  }
		  deephyActionButton("Send Feedback") {
			mail(
			  address = "deephys@mit.edu",
			  subject = "This visualizer is so cool!",
			  body = "What I like about this tool:\n\n\n\nHow I think it can be improved:\n\n"
			)
		  }.apply{
			prefHeightProperty.bind(settButton.heightProperty)
		  }
		  +settButton
		}
	  }

	  h {

		vgrow = ALWAYS

		fillHeightProperty.value = true

		+navBox!!

		scrollpane<VBoxWrapperImpl<NW>> {
		  hgrow = ALWAYS
		  hbarPolicy = NEVER
		  isFitToWidth = true

		  content = visBox!!
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