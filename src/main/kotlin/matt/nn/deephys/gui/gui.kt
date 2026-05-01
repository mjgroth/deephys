@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER", "SpellCheckingInspection")
@file:OptIn(ExperimentalAtomicApi::class)

package matt.nn.deephys.gui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import matt.async.thread.daemon
import matt.auto.desktop.SuspendingShellBasedDesktopAutomationContext
import matt.auto.desktop.awt.AwtBasedDesktopAutomationContext
import matt.compose.app.myApplication
import matt.compose.controls.desktop.scroll.verticallyScrollable
import matt.compose.controls.window.MyWindow
import matt.compose.controls.window.main.MyMainWindow
import matt.compose.controls.window.state.MyWindowState
import matt.compose.graphics.image.j.MyImage
import matt.compose.graphics.label.LabeledOnTheLeft
import matt.compose.graphics.text.style.style.LocalTextStyler
import matt.compose.snap.withSafeMutableSnapshot
import matt.compose.state.shortcuts.rememberMutableStateOf
import matt.compose.state.win.HardWindowState
import matt.exec.app.myVersion
import matt.file.JioFile
import matt.file.commons.desktop.PLATFORM_INDEPENDENT_APP_SUPPORT_FOLDER
import matt.file.construct.toJioFile
import matt.file.ext.j.mkFold
import matt.http.internet.TheInternet
import matt.http.internet.isAvailable
import matt.http.tryHttp
import matt.image.icon.ICON_SIZES
import matt.lang.anno.SeeUrl
import matt.lang.anno.optin.ExperimentalMattCode
import matt.lang.err.unsafeError
import matt.lang.err.unsafeReturningErr
import matt.lang.shutdown.ShutdownScheduler
import matt.model.code.successorfail.requireSuccess
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.model.k.file.file.MacFileSystem
import matt.model.k.kstruct.mod.uniqueCamelCaseName
import matt.model.k.log.Logger
import matt.model.k.osi.url.MURL
import matt.nn.deephys.gui.DeephysArg.`erase-settings`
import matt.nn.deephys.gui.DeephysArg.`erase-state`
import matt.nn.deephys.gui.DeephysArg.reset
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
import matt.nn.deephys.gui.global.DEEPHYS_FONT_DEFAULT
import matt.nn.deephys.gui.global.DeephyActionButton
import matt.nn.deephys.gui.global.DeephyButton
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.modelvis.ModelVisualizerState
import matt.nn.deephys.gui.navbox.NavBox
import matt.nn.deephys.gui.navbox.NavBoxTab
import matt.nn.deephys.gui.navbox.zoo.ZooExample
import matt.nn.deephys.gui.settings.DeephySettingsNode
import matt.nn.deephys.gui.settings.gui.SettingsWindow
import matt.nn.deephys.gui.visbox.VisBox
import matt.nn.deephys.init.gearImage
import matt.nn.deephys.load.loadCbor
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
import matt.nn.deephys.version.VersionChecker
import matt.rstruct.desktop.modId
import matt.rstruct.loader.desktop.systemResourceLoader
import matt.sys.thisMachine
import java.net.URI
import kotlin.concurrent.atomics.ExperimentalAtomicApi

val DEEPHY_USER_DATA_DIR by lazy {
    PLATFORM_INDEPENDENT_APP_SUPPORT_FOLDER.toJioFile().mkFold("Deephys")
}
val DEEPHYS_LOG_FOLDER by lazy {
    DEEPHY_USER_DATA_DIR["log"]
}

enum class DeephysArg {
    `erase-state`, `erase-settings`, reset
}
typealias DeephysArgs = List<DeephysArg>

@OptIn(ExperimentalMattCode::class)
class DeephysApp {

    val selectedNavTab = mutableStateOf(NavBoxTab.NeuronalActivityZoo)

    context(_: ShutdownScheduler, _: Logger)
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

    context(_: ShutdownScheduler, _: Logger)
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
                val _ =
                    daemon(name = "Stage Title Loader") {
                        try {
                            stageTitle.putLoadedValue("${modId.appName} $myVersion")
                        } finally {
                            if (!stageTitle.isDoneOrCancelled()) {
                                stageTitle.cancel("${Thread.currentThread().name} failed")
                            }
                        }
                    }

                val _ =
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

    val testReadyDSetViewsBbox = MutableSharedFlow<DSetViewsState>()
    private val readyForConfiguringWindowFromTest = LoadedValueSlot<Any>()
    val testReadyScene = LoadedValueSlot<Unit>()

    val showNavBox = mutableStateOf(false)
    fun showDemos() {
        withSafeMutableSnapshot {
            showNavBox.value = true
            selectedNavTab.value = NavBoxTab.NeuronalActivityZoo
        }
    }

    class ZooDownloaderState(
        val demo: ZooExample,
        val scope: CoroutineScope
    ) {
        val total = demo.testURLs.size + 1
        val done = mutableStateOf(0)
        val progress = derivedStateOf { done.value / total }
        private suspend fun download(
            name: String,
            url: MURL
        ): JioFile =
            with(MacFileSystem) {
                val f = matt.file.ext.j.createTempFile(name, suffix = "")
                f.writeBytes(tryHttp(url).requireSuccess().bytes())
                withSafeMutableSnapshot {
                    done.value += 1
                }
                f
            }

        val modelFile =
            scope.async {
                download("model_${demo.name}", demo.modelURL)
            }
        val testFiles =
            demo.testURLs.mapIndexed { i, testURL ->
                scope.async {
                    download("test_$i", testURL)
                }
            }
    }
    suspend fun openZooDemo(
        state: ZooDownloaderState
    ) {
        val demo = state.demo

        if (!TheInternet().isAvailable()) {
            unsafeError("No internet connection")
            return
        }
        state.modelFile.await()
        state.testFiles.forEach {
            it.await()
        }

        /*root.findRecursivelyFirstOrNull<DSetViewsVBox>()?.removeAllTests()*/
    }

    var dSetViewsState: DSetViewsState? = null

    @Suppress("UnusedParameter")
    context(_: Logger)
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

        val auto = SuspendingShellBasedDesktopAutomationContext

        dSetViewsState =
            DSetViewsState(
                deephyState = deephyState,
                modelVisualizer = ModelVisualizerState()
            )

        myApplication(
            appName = "Deephys",
            logFile = DEEPHYS_LOG_FOLDER["log"],
            appId = modId.uniqueCamelCaseName,
            suspendingAutomationContext = auto
        ) { scope ->
            LaunchedEffect(scope) {
                VersionChecker.start(scope)
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
                        LabeledOnTheLeft("This was the \"stage icon\": ") {
                            MyImage(
                                systemResourceLoader().resourceURL("logo_${ICON_SIZES.random()}.png").toString().let(::URI)
                            )
                        }
                        val showSettingsWindow = rememberMutableStateOf(false)
                        readyForConfiguringWindowFromTest.putLoadedValue(Unit)

                        val settButton = SettingsWindow(settingsNode.settings)

                        unsafeError("visBox =")

                        VisBox(
                            app = this@DeephysApp,
                            settings = settingsNode.settings,
                            loadedModel = deephyState.loadedModel,
                            state = deephyState,
                            dsetViews = dSetViewsState!!,
                            modelVisualizerState = dSetViewsState!!.modelVisualizer,
                            showSettingsWindow = { showSettingsWindow.value = true }
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
                                    """.trimIndent()
                                )
                                /*spacing = DEEPHYS_SYMBOL_SPACING*/
                                DeephyButton(
                                    Modifier.height(
                                        unsafeReturningErr<Dp>("settButton.heightProperty")
                                    ),
                                    s = "Report Bug"
                                ) {
                                    unsafeError(
                                        """
                                         isDisable = true
                                        daemon(name = "report bug") {
                                            /*ON LINUX THIS MUST OCCUR IN ANOTHER THREAD*/
                                            openNewYouTrackIssue(
                                                summary = "Bug Report",
                                                description = ""
                                            )
                                            isDisable = false
                                        }
                                        """.trimIndent()
                                    )
                                }
                                DeephyButton(
                                    Modifier.height(
                                        unsafeReturningErr<Dp>("settButton.heightProperty")
                                    ),
                                    s = "Send Feedback"
                                ) {
                                    unsafeError(
                                        """
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
                                        """.trimIndent()
                                    )
                                }
                                unsafeError("+settButton")
                            }
                        }

                        Row {
                            unsafeError(
                                """
                            vgrow = ALWAYS
                            fillHeightProperty.value = true
                                """.trimIndent()
                            )
                            with(auto) {
                                if (showNavBox.value) NavBox(
                                    app = this@DeephysApp,
                                    deephyState = deephyState,
                                    dSetViewsState = dSetViewsState!!
                                )
                            }
                            Box(Modifier.verticallyScrollable()) {
                                unsafeError(
                                    """
                                       hgrow = ALWAYS
                                hbarPolicy = NEVER
                                isFitToWidth = true

                                content = visBox!!
                                    """.trimIndent()
                                )
                            }
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
            }
        }
    }
}
