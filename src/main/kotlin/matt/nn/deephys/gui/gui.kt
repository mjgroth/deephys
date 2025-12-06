@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER", "SpellCheckingInspection")
@file:OptIn(ExperimentalAtomicApi::class)

package matt.nn.deephys.gui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import matt.async.thread.daemon
import matt.async.thread.pool.DaemonPoolExecutor
import matt.auto.desktop.awt.AwtBasedDesktopAutomationContext
import matt.compose.app.myApplication
import matt.compose.controls.window.MyWindow
import matt.compose.controls.window.main.MyMainWindow
import matt.compose.controls.window.state.MyWindowState
import matt.compose.graphics.image.j.MyImage
import matt.compose.graphics.layout.AlignedRow
import matt.compose.graphics.text.MyText
import matt.compose.graphics.text.style.style.LocalTextStyler
import matt.compose.snap.withSafeMutableSnapshot
import matt.compose.state.shortcuts.rememberMutableStateOf
import matt.compose.state.win.HardWindowState
import matt.exec.app.myVersion
import matt.file.JioFile
import matt.file.commons.desktop.PLATFORM_INDEPENDENT_APP_SUPPORT_FOLDER
import matt.file.commons.logctx.LogContext1
import matt.file.ext.j.mkFold
import matt.file.thismachine.thisMachine
import matt.file.toJioFile
import matt.http.internet.TheInternet
import matt.http.internet.isAvailable
import matt.image.icon.ICON_SIZES
import matt.lang.anno.SeeUrl
import matt.lang.anno.optin.ExperimentalMattCode
import matt.lang.common.unsafeError
import matt.lang.shutdown.TypicalShutdownContext
import matt.lang.sync.common.SimpleReferenceMonitor
import matt.lang.sync.common.withLock
import matt.model.code.mod.uniqueCamelCaseName
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.nn.deephys.gui.DeephysArg.`erase-settings`
import matt.nn.deephys.gui.DeephysArg.`erase-state`
import matt.nn.deephys.gui.DeephysArg.reset
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
import matt.nn.deephys.gui.global.DEEPHYS_FONT_DEFAULT
import matt.nn.deephys.gui.global.DeephyActionButton
import matt.nn.deephys.gui.global.DeephyButton
import matt.nn.deephys.gui.global.DeephysLabel
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.navbox.NavBoxTab
import matt.nn.deephys.gui.navbox.zoo.ZooExample
import matt.nn.deephys.gui.settings.DeephySettingsNode
import matt.nn.deephys.gui.unsafemigration.VisBox
import matt.nn.deephys.gui.unsafemigration.unsafeComposable
import matt.nn.deephys.init.gearImage
import matt.nn.deephys.load.loadCbor
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
import matt.nn.deephys.version.VersionChecker
import matt.obs.prop.writable.BindableProperty
import matt.obs.prop.writable.v
import matt.obs.subscribe.Pager
import matt.prim.common.exportfromlang.model.file.MacFileSystem
import matt.rstruct.desktop.modId
import matt.rstruct.loader.desktop.systemResourceLoader
import java.net.URI
import java.net.URL
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.path.outputStream

val DEEPHY_USER_DATA_DIR by lazy {
    PLATFORM_INDEPENDENT_APP_SUPPORT_FOLDER.toJioFile().mkFold("Deephys")
}
val DEEPHYS_LOG_CONTEXT by lazy {
    LogContext1(DEEPHY_USER_DATA_DIR)
}

enum class DeephysArg {
    `erase-state`, `erase-settings`, reset
}
typealias DeephysArgs = List<DeephysArg>

@OptIn(ExperimentalMattCode::class)
class DeephysApp {

    val selectedNavTab = mutableStateOf(NavBoxTab.NeuronalActivityZoo)

    context(_: TypicalShutdownContext)
    fun boot2(
        settingsNode: DeephySettingsNode,
        deephyState: DeephyState,
        args: DeephysArgs
    ): Unit =
        boot(
            args = args,
            settingsNode = settingsNode,
            deephyState = deephyState
        )

    context(_: TypicalShutdownContext)
    /*invoked directly from test, in case I ever want to return something*/
    fun boot(
        args: DeephysArgs,
        settingsNode: DeephySettingsNode,
        deephyState: DeephyState
    ) {
        when (args.size) {
            1 if args[0] == `erase-state`    -> {
                unsafeError("deephyState.delete()")
            }

            1 if args[0] == `erase-settings` -> {
                unsafeError("settingsNode.delete()")
            }

            1 if args[0] == reset            -> {
                unsafeError(
                    """
                deephyState.delete()
                settingsNode.delete()    
                    """.trimIndent()
                )
            }

            else                             -> {
                daemon(name = "Stage Title Loader") {
                    try {
                        stageTitle.putLoadedValue("${modId.appName} $myVersion")
                    } finally {
                        if (!stageTitle.isDoneOrCancelled()) {
                            stageTitle.cancel("${Thread.currentThread().name} failed")
                        }
                    }
                }

                daemon("initializeWhatICan Thread") {
                    gearImage.startLoading()
                }

                val lastVersion = deephyState.lastVersionOpened.value
                val thisVersion = modId.version.toString()
                var openedNewVersion = false
                if (lastVersion != thisVersion) {
                    deephyState.lastVersionOpened.value = thisVersion
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
                    openedNewVersion = openedNewVersion,
                    deephyState = deephyState
                )
            }
        }
    }

    val stageTitle = LoadedValueSlot<String>()

    @ExperimentalMattCode(incomplete = "might not have cancelled all of the latches")
    fun cancelAllLatches(cause: Throwable) {
        testReadyScene.cancel(cause)
        readyForConfiguringWindowFromTest.cancel(cause)
        stageTitle.cancel(cause)
    }

    val testReadyDSetViewsBbox = Pager<DSetViewsState>()
    private val readyForConfiguringWindowFromTest = LoadedValueSlot<Any>()
    val testReadyScene = LoadedValueSlot<Unit>()

    var visBox: VisBox? = null
    private val showNavBox = mutableStateOf(false)
    fun showDemos() {
        withSafeMutableSnapshot {
            showNavBox.value = true
            selectedNavTab.value = NavBoxTab.NeuronalActivityZoo
        }
    }
    fun openZooDemo(demo: ZooExample) {

        if (runBlocking {  !TheInternet().isAvailable() }) {
            unsafeError("No internet connection")
            return
        }

        val pool = DaemonPoolExecutor()

        val modelURL = URI(demo.modelURL.path).toURL()
        val testURLs = demo.testURLs.map { URI(it.path).toURL() }

        val total = testURLs.size + 1
        val done = AtomicInt(0)
        val progress = BindableProperty(0.0)

        val monitor = SimpleReferenceMonitor()

        fun download(
            name: String,
            url: URL
        ): JioFile =
            with(MacFileSystem) {
                val f = matt.file.ext.j.createTempFile(name, suffix = "")
                url.openStream().use { downloadStream ->
                    f.outputStream().use { writeStream ->
                        downloadStream.transferTo(writeStream)
                    }
                }
                done.addAndFetch(1)
                monitor.withLock {
                    progress v done.load().toDouble() / total
                }
                f
            }

        val modelFile =
            pool.submit {
                download("model_${demo.name}", modelURL)
            }

        val testFiles =
            testURLs.mapIndexed { i, testURL ->
                pool.submit {
                    download("test_$i", testURL)
                }
            }

        unsafeComposable {
            Column {

                DeephysLabel("Downloading ${demo.name}...")

                val prog =
                    LinearProgressIndicator(
                        progress = {
                            progress.value.toFloat()
                        }
                    )

                unsafeError(
                    """
                    DeephysLabel("Loading Files... (${done.load()}/$total)") {
                        if (done.get() == total) {
                            stage!!.close()
                            showNavBox.value = false
                            val theVisBox = visBox ?: err("no visBox!")
                            theVisBox.load(
                                modelFile = modelFile.get(),
                                testFiles = testFiles.map { it.get() }
                            )
                        }
                    }           
                    """.trimIndent()
                )
            }.apply {
                unsafeError(
                    """
                    openInNewWindow(
                        showMode = SHOW_AND_WAIT,
                        wMode = NOTHING,
                        alwaysOnTop = true
                    ) {
                    }            
                    """.trimIndent()
                )
            }
        }

        modelURL.openStream()

        /*root.findRecursivelyFirstOrNull<DSetViewsVBox>()?.removeAllTests()*/
    }

    @Suppress("UnusedParameter")
    private fun startDeephyApp(
        settingsNode: DeephySettingsNode,
        settingsDidReset: Boolean,
        @Suppress("UNUSED_PARAMETER")
        openedNewVersion: Boolean,
        deephyState: DeephyState
    ) {

        val flow =
            snapshotFlow {
                deephyState.model.value
            }

        myApplication(
            appName = "Deephys",
            logFile = null,
            appId = modId.uniqueCamelCaseName
        ) { scope ->

            LaunchedEffect(scope) {
                scope.launch(Dispatchers.IO) {
                    flow.collect { f ->
                        deephyState.loadedModel.value = null
                        deephyState.loadedModel.value = f?.run { toJioFile().withinFileSystem(thisMachine.fileSystemFor(f.path)).loadCbor<Model>() }!!
                    }
                }
            }

            val myWindowState =
                remember(scope) {
                    MyWindowState(
                        state =
                            HardWindowState(
                                key = "deephys-main-window",
                                appInstanceNumber = 0 /*STUPID*/
                            ).createReal(scope)
                    )
                }

            LocalTextStyler(
                fontFamily = DEEPHYS_FONT_DEFAULT
            ) {

                val myStageTitle = stageTitle.await()

                MyMainWindow(
                    windowState = myWindowState,
                    title = myStageTitle
                ) {

                    if (settingsDidReset) {
                        val openWelcomeWindow = rememberMutableStateOf(true)
                        MyWindow(
                            visible = openWelcomeWindow.value,
                            alwaysOnTop = true,
                            onCloseRequest = {
                                openWelcomeWindow.value = false
                            }
                        ) {
                            Column {
                                DeephysText(s = "Welcome to Deephys")
                                if (settingsDidReset) {
                                    DeephysText(s = "Your settings have been reset due to the new update.")
                                }
                                DeephyActionButton("OK") {
                                    openWelcomeWindow.value = false
                                }
                            }
                        }
                    }
                    Column(
                        @SeeUrl("https://www.theverge.com/2013/7/15/4523668/11-inch-macbook-air-review")
                        Modifier
                            .requiredWidthIn(
                                min = 1000.0.dp
                            )
                            .requiredHeightIn(
                                min = 750.dp
                            )
                    ) {
                        AlignedRow(
                            horizontalArrangement = Arrangement.Center
                        ) {
                            MyText("This was the \"stage icon\": ")
                            MyImage(systemResourceLoader().resourceURL("logo_${ICON_SIZES.random()}.png").toString().let(::URI))
                        }
                        unsafeError(
                            """
                                    
                                readyForConfiguringWindowFromTest.putLoadedValue(Unit)
                                val settButton = SettingsWindow(settingsNode.settings).button(this)




                                visBox =
                                    VisBox(
                                        app = this@DeephysApp,
                                        settings = settingsNode.settings
                                    )

                            """.trimIndent()
                        )

                        unsafeError(
                            """
                                  hotkeys {
                            COMMA.meta {
                                settButton.fire()
                            }
                        } 
                            """.trimIndent()
                        )

                        Row {

                            DeephyButton(
                                Icons.Default.Menu
                            ) {
                                showNavBox.value = !showNavBox.value
                            }
                            Row {
                                unsafeError(
                                    """
                                hgrow = ALWAYS
                                alignment = Pos.CENTER_RIGHT
                                /*spacing = DEEPHYS_SYMBOL_SPACING*/
                                DeephyButton("Report Bug") {
                                    setOnAction {
                                        isDisable = true
                                        daemon(name = "report bug") {
                                            /*ON LINUX THIS MUST OCCUR IN ANOTHER THREAD*/
                                            openNewYouTrackIssue(
                                                summary = "Bug Report",
                                                description = ""
                                            )
                                            isDisable = false
                                        }
                                    }
                                }.apply {
                                    prefHeightProperty.bind(settButton.heightProperty)
                                }
                                DeephyButton("Send Feedback") {

                                    setOnAction {
                                        isDisable = true
                                        daemon(name = "send feedback") {
                                            /*ON LINUX THIS MUST OCCUR IN ANOTHER THREAD*/
                                            mail(
                                                address = "deephys@mit.edu",
                                                subject = "This visualizer is so cool!",
                                                body = "What I like about this tool:\n\n\n\nHow I think it can be improved:\n\n"
                                            )
                                            isDisable = false
                                        }
                                    }
                                }.apply {
                                    prefHeightProperty.bind(settButton.heightProperty)
                                }
                                +settButton          
                                    """.trimIndent()
                                )
                            }
                        }

                        Row {
                            unsafeError(
                                """
                                        vgrow = ALWAYS

                            fillHeightProperty.value = true

                            if (showNavBox.value) NavBox(this@DeephysApp)

                            scrollpane<VBoxWrapperImpl<NW>> {
                                hgrow = ALWAYS
                                hbarPolicy = NEVER
                                isFitToWidth = true

                                content = visBox!!
                            } 
                                """.trimIndent()
                            )
                        }

                        /*
                         vbox<NodeWrapper> {
                           vgrow = ALWAYS
                         }*/

                        Column(
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            with(AwtBasedDesktopAutomationContext) {
                                VersionChecker.statusNode()
                            }
                        }
                    }
                }

        /*not currently using this, because after making scroll bars transparent I found out that nothing was in fact being laid out underneath them, so it was just creating a weird space. Search for search key FRHWOIH83RH3URUG34TGOG34G934G


          scene!!.stylesheets.add(ClassLoader.getSystemResource("deephys.css").toString())*/

                testReadyScene.putLoadedValue(Unit)

                println("put loaded scene")

                VersionChecker.checkForUpdatesInBackground()
                unsafeError(
                    """
                    logContext = DEEPHYS_LOG_CONTEXT
                    """.trimIndent()
                )
            }
        }
    }
}
