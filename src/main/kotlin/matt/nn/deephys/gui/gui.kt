@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED", "UNUSED_VARIABLE", "UNUSED_PARAMETER", "SpellCheckingInspection", "unused")
@file:OptIn(ExperimentalAtomicApi::class)

package matt.nn.deephys.gui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import matt.async.thread.daemon
import matt.async.thread.pool.DaemonPoolExecutor
import matt.compose.app.myApplication
import matt.compose.controls.window.MyWindow
import matt.compose.controls.window.main.MyMainWindow
import matt.compose.controls.window.state.MyWindowState
import matt.compose.graphics.text.style.style.LocalTextStyler
import matt.compose.state.rememberMutableStateOf
import matt.compose.state.win.HardWindowState
import matt.exec.app.myVersion
import matt.file.JioFile
import matt.file.commons.desktop.PLATFORM_INDEPENDENT_APP_SUPPORT_FOLDER
import matt.file.commons.logctx.LogContext1
import matt.file.ext.j.mkFold
import matt.file.toJioFile
import matt.http.internet.TheInternet
import matt.http.internet.isAvailable
import matt.lang.anno.SeeURL
import matt.lang.anno.optin.ExperimentalMattCode
import matt.lang.common.unsafeErr
import matt.lang.j.sync
import matt.lang.model.file.MacFileSystem
import matt.lang.shutdown.TypicalShutdownContext
import matt.lang.sync.common.SimpleReferenceMonitor
import matt.model.code.mod.uniqueCamelCaseName
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.nn.deephys.gui.DeephysArg.`erase-settings`
import matt.nn.deephys.gui.DeephysArg.`erase-state`
import matt.nn.deephys.gui.DeephysArg.reset
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
import matt.nn.deephys.gui.global.DEEPHYS_FONT_DEFAULT
import matt.nn.deephys.gui.global.DeephyActionButton
import matt.nn.deephys.gui.global.DeephysLabel
import matt.nn.deephys.gui.global.DeephysText
import matt.nn.deephys.gui.navbox.zoo.ZooExample
import matt.nn.deephys.gui.settings.DeephySettingsNode
import matt.nn.deephys.gui.unsafemigration.ToggleButtonWrapper
import matt.nn.deephys.gui.unsafemigration.VisBox
import matt.nn.deephys.gui.unsafemigration.unsafeComposable
import matt.nn.deephys.init.initializeWhatICan
import matt.nn.deephys.state.DeephyState
import matt.nn.deephys.version.VersionChecker
import matt.obs.prop.writable.BindableProperty
import matt.obs.subscribe.Pager
import matt.rstruct.desktop.modId
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





class DeephysApp {

    var showDemosTab: ToggleButtonWrapper? = null





    context(TypicalShutdownContext)
    fun boot2(
        settingsNode: DeephySettingsNode,
        args: DeephysArgs
    ): Unit =
        boot(
            args = args,
            settingsNode = settingsNode
        )

    context(TypicalShutdownContext)
    /*invoked directly from test, in case I ever want to return something*/
    fun boot(
        args: DeephysArgs,
        settingsNode: DeephySettingsNode = DeephySettingsNode()
    ) {
        when (args.size) {
            1 if args[0] == `erase-state`    -> {
                DeephyState.delete()
            }

            1 if args[0] == `erase-settings` -> {
                settingsNode.delete()
            }

            1 if args[0] == reset            -> {
                DeephyState.delete()
                settingsNode.delete()
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
                    initializeWhatICan()
                }

                val lastVersion = DeephyState.lastVersionOpened.value!!
                val thisVersion = modId.version.toString()
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
    private val testReadyScene = LoadedValueSlot<Unit>()

    var visBox: VisBox? = null
    private val showNavBox = mutableStateOf(false)
    fun showDemos() {
        showNavBox.value = true
        unsafeErr(
            """
            showDemosTab!!.isSelected = true    
            """.trimIndent()
        )
    }
 /*   fun showDemos() {

    }*/

    fun openZooDemo(demo: ZooExample) {


        if (runBlocking {  !TheInternet().isAvailable() }) {
            unsafeErr("No internet connection")
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
                monitor.sync {
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

                unsafeErr(
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
                unsafeErr(
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
        openedNewVersion: Boolean
    ) {

        val myWindowState =
            MyWindowState(
                state =
                    HardWindowState(
                        key = "deephys-main-window",
                        appInstanceNumber = 0 /*STUPID*/
                    )
            )

        myApplication(
            appName = "Deephys",
            logFile = null,
            appId = modId.uniqueCamelCaseName
        ) {


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
                                DeephysText("Welcome to Deephys")
                                if (settingsDidReset) {
                                    DeephysText("Your settings have been reset due to the new update.")
                                }
                                DeephyActionButton("OK") {
                                    openWelcomeWindow.value = false
                                }
                            }
                        }
                    }
                    Column(
                        @SeeURL("https://www.theverge.com/2013/7/15/4523668/11-inch-macbook-air-review")
                        Modifier
                            .requiredWidthIn(
                                min = 1000.0.dp
                            )
                            .requiredHeightIn(
                                min = 750.dp
                            )
                    ) {
                        unsafeErr(
                            """
                                            AlignedRow(
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    MyText("This was the \"stage icon\": ")
                                    MyImage(systemResourceLoader().resourceURL("logo_$it.png").toString().let(::URI))
                                }
                                readyForConfiguringWindowFromTest.putLoadedValue(Unit)
                                val settButton = SettingsWindow(settingsNode.settings).button(this)




                                visBox =
                                    VisBox(
                                        app = this@DeephysApp,
                                        settings = settingsNode.settings
                                    )


                                unsafeErr(
                                    ""${'"'}
                                        
                                                                hotkeys {
                                                                    COMMA.meta {
                                                                        settButton.fire()
                                                                    }
                                                                }

                                    ""${'"'}.trimIndent()
                                )



                                Row {

                                    DeephyButton(
                                        Icons.Default.Menu
                                    ) {
                                        showNavBox.value = !showNavBox.value
                                    }
                                    Row {
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
                                    }
                                }

                                Row {

                                    vgrow = ALWAYS

                                    fillHeightProperty.value = true

                                    if (showNavBox.value) NavBox(this@DeephysApp)

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

                                Column(
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    +VersionChecker.statusNode()
                                }
                            """.trimIndent()
                        )
                    }
                }



        /*not currently using this, because after making scroll bars transparent I found out that nothing was in fact being laid out underneath them, so it was just creating a weird space. Search for search key FRHWOIH83RH3URUG34TGOG34G934G


          scene!!.stylesheets.add(ClassLoader.getSystemResource("deephys.css").toString())*/

                testReadyScene.putLoadedValue(Unit)

                println("put loaded scene")

                VersionChecker.checkForUpdatesInBackground()
                unsafeErr(
                    """
                    logContext = DEEPHYS_LOG_CONTEXT
                    """.trimIndent()
                )
            }
        }
    }
}
