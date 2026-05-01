@file:Suppress("UNUSED_VARIABLE")

package matt.nn.deephys.test.deephys.tester

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import matt.async.thread.namedThread
import matt.file.common.toAbsLinuxFile
import matt.file.commons.desktop.DEEPHYS_TEST_RESULT_JSON
import matt.file.commons.reg.RegisteredFolder
import matt.file.construct.mFile
import matt.file.construct.toJioFile
import matt.file.ext.j.mkparents
import matt.file.model.file.types.Cbor
import matt.file.types.forceType
import matt.json.prim.loadJson
import matt.json.prim.saveJson
import matt.lang.anno.optin.ExperimentalMattCode
import matt.lang.anno.optin.ShadowsExtensionBug
import matt.lang.err.unsafeError
import matt.lang.err.unsafeReturningErr
import matt.lang.shutdown.ShutdownScheduler
import matt.lang.sysprop.common.value
import matt.lang.sysprop.expects.RuntimePropertyProvider
import matt.log.j.DefaultLogger
import matt.log.profile.data.TestResults
import matt.log.profile.data.TestSession
import matt.log.profile.real.Profiler
import matt.log.profile.stopwatch.tic
import matt.log.profile.yk.YourKit
import matt.log.report.desktop.MemReport
import matt.model.code.errreport.common.reportAndReThrowErrorsBetter
import matt.model.data.bytesize.mebibytes
import matt.model.k.file.file.MacFileSystem
import matt.model.obj.text.doesNotExist
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.DeephysArg.reset
import matt.nn.deephys.gui.settings.DeephySettingsNode
import matt.nn.deephys.load.cache.DeephysCacheManager
import matt.nn.deephys.state.DeephyState
import matt.nn.deephys.test.deephys.DeephysTestData
import matt.nn.deephys.test.deephys.NUM_IM_CLICKS
import matt.nn.deephys.test.deephys.NUM_SLICE_CLICKS
import matt.nn.deephys.test.deephys.TestDeephys
import matt.nn.deephys.test.deephys.WAIT_FOR_GUI_INTERVAL
import matt.service.action.NoActionAbilities
import matt.sys.j.runtime.RUNTIME
import matt.test.assertions.assertTrueLazyMessage
import matt.test.prop.TestProperties
import matt.test.prop.j.CommonJTestProperties
import matt.time.dur.sleep
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMattCode::class)
class DeephysTestSession(
    private val profiler: Profiler,
    processReaper: ShutdownScheduler
) {

    private val app by lazy {
        DeephysApp()
    }

    private val mainStage by lazy {
        unsafeError(
            """
            app.readyForConfiguringWindowFromTest.await()    
            """.trimIndent()
        )
    }

    private val settingsNode = DeephySettingsNode()
    private val deephyState = DeephyState()

    init {
        reportAndReThrowErrorsBetter {

            context(processReaper, DefaultLogger) {
                app.boot2(settingsNode = settingsNode, args = listOf(reset), deephyState = deephyState) /*need this so tests are deterministic*/
                val _ =
                    namedThread(name = "App Launcher") {
                        try {
                            app.boot2(
                                args = listOf(),
                                settingsNode = settingsNode,
                                deephyState = deephyState
                            )
                        } catch (e: Throwable) {
                            println("CANCELLING ALL LATCHES")
                            app.cancelAllLatches(e)
                        }
                    }
            }

            @Suppress("unused")
            val theMainStage = mainStage
            unsafeError(
                """
                runLaterReturn {
                    theMainStage.width = MAC_MAYBE_MIN_SCREEN_SIZE.width
                    theMainStage.height = MAC_MAYBE_MIN_SCREEN_SIZE.height
                    theMainStage.centerOnScreen()
                }        
                """.trimIndent()
            )
        }
    }

    private val confirmService by lazy {
        /*replace with the Compose implementation, the shell implementation, or isn't there an alternative suspending interface now? or something once I migrate*/
        NoActionAbilities
    }

    @Suppress("unused")
    fun testConfirmation(
        prompt: String,
        force: Boolean = false
    ) =
        runBlocking {
            with(RuntimePropertyProvider) {
                if (force || TestProperties.ManualTests.value()) matt.test.assertions.testConfirmation(prompt, confirmService) else Unit
            }
        }

    fun testHasCorrectTitle() =
        assertEquals(
            expected = app.stageTitle.await(),
            actual = unsafeReturningErr("mainStage.title")
        )

    fun testFitsInSmallestScreen() {
        unsafeError(
            $$"""
            val w = mainStage.width
            val h = mainStage.height
            assertTrueLazyMessage(
                w == MAC_MAYBE_MIN_SCREEN_SIZE.width && h == MAC_MAYBE_MIN_SCREEN_SIZE.height
            ) {
                "mainStage .width=$w .height= $h}"
            }     
            """.trimIndent()
        )
    }

    init {
        val _ = RegisteredFolder.Main.DEEPHYS_TEST_RESULT_JSON.mkparents()
    }

    @OptIn(ShadowsExtensionBug::class)
    private val sessionList =
        if (RegisteredFolder.Main.DEEPHYS_TEST_RESULT_JSON.doesNotExist() || RegisteredFolder.Main.DEEPHYS_TEST_RESULT_JSON.readText().isBlank()) {
            mutableListOf<TestSession>()
        } else {
            RegisteredFolder.Main.DEEPHYS_TEST_RESULT_JSON.loadJson()
        }

    private val mySession = TestSession().also { sessionList.add(it) }

    @Suppress("UnusedParameter")
    fun loadDataAndCheckItWasFastEnough(
        key: String,
        @Suppress("unused") testData: DeephysTestData,
        maxTime: Duration
    ) {
        TestDeephys.sampleRam()
        val t = tic("runThroughFeatures")

        @IgnorableReturnValue
        fun tocAndSampleRam(marker: String): Duration? {
            val r = t.toc(marker)
            TestDeephys.sampleRam()
            return r
        }
        @Suppress("unused")
        val scene = app.testReadyScene.await()
        tocAndSampleRam("got scene")
        unsafeError(
            """
            val root = scene.root
            """.trimIndent()
        )
        runBlocking {

            val _ =
                profiler.record {
                    app.dSetViewsState!!.removeAllTests()
                    app.dSetViewsState!!.deephyState.model.value = testData.model.toAbsLinuxFile()
                    app.testReadyDSetViewsBbox.first()

                    tocAndSampleRam("GUI ready")

                    val dSetViewsBox = app.dSetViewsState!!

                    tocAndSampleRam("found dSetViewsBox")

                    val testViewersAndFiles =
                        testData.tests.map {
                            dSetViewsBox.addTest() to it
                        }

                    tocAndSampleRam("added tests")

                    testViewersAndFiles.forEach {
                        it.first.setCborFile(
                            (mFile(it.second.abspath, MacFileSystem)).forceType(Cbor).toJioFile()
                        )
                    }

                    tocAndSampleRam("set test files")

                    testViewersAndFiles.forEachIndexed { index, it ->
                        val _ = it.first.testData.value!!.awaitFinishedTest()
                        tocAndSampleRam("test ${index + 1} finished loading")
                    }
                    val firstViewer = testViewersAndFiles.first().first
                    firstViewer.manualLayerSelected.value =
                        dSetViewsBox
                            .modelVisualizer
                            .model
                            .value!!
                            .resolvedLayers
                            .first()
                            .interTest

                    tocAndSampleRam("selected layer")

                    unsafeError(
                        """
            runLaterReturn {
                dSetViewsBox.selectViewerToBind(firstViewer, makeInDToo = true)
            }        
                        """.trimIndent()
                    )
                }
            val totalTime = tocAndSampleRam("set binding")!!
            mySession.tests.add(
                TestResults(
                    name = key,
                    loadMillis = totalTime.inWholeMilliseconds
                )
            )
            @Suppress("RedundantValueArgument")
            RegisteredFolder.Main.DEEPHYS_TEST_RESULT_JSON.saveJson(sessionList, pretty = true)
            with(RuntimePropertyProvider) {
                assertTrueLazyMessage(
                    !CommonJTestProperties.TestPerformance.value()
                        || totalTime < maxTime
                ) {
                    "took to long to load: took=$totalTime expected=$maxTime"
                }
            }
            TestDeephys.sampleRam()
        }
    }

    fun runThroughByImageView() {
        println("awaiting scene to be ready...")
        var clicked = 0
        unsafeError(
            $$"""
            val scene = app.testReadyScene.await()
            println("automatically clicking through $NUM_IM_CLICKS images")
            val root = scene.root
            val dSetViewsBox = root.findRecursivelyFirstOrNull<DSetViewsVBox>()!!
            val viewers = dSetViewsBox.children
            val firstViewer = viewers.first()
            val secondViewer = viewers[1]
            runLaterReturn {
                val im0: DeephyImage<*> = firstViewer.testData.value!!.postDtypeTestLoader.awaitRequireSuccessful().awaitImage(0)
                firstViewer.navigateTo(im0)
            }
            """.trimIndent()
        )
        sleep(WAIT_FOR_GUI_INTERVAL)
        while (clicked < NUM_IM_CLICKS) {
            unsafeError(
                $$"""
                       val firstViewerSelection = firstViewer.imageSelection.value
            val allImViews = firstViewer.recurseSelfAndChildNodes<DeephyImView>()
            val dIm =
                allImViews.firstOrNull {
                    it.weakIm.deref()!! != firstViewerSelection
                } ?: run {
                    val imViews = allImViews.toList()
                    error(
                        "could not find an image different from $firstViewerSelection, all=${
                    imViews.map { it.weakIm.deref()?.imageID }
                        .elementsToString()
                }, imViews=${imViews.size}, clicked=$clicked"
                    )
                }
                """.trimIndent()
            )

            println("clicking an image...")

            unsafeError(
                """
                        val secondViewerImagesBefore: List<Int> =
                runLaterReturn {
                    secondViewer.recurseSelfAndChildNodes<DeephyImView>().map {
                        it.weakIm.deref()!!.imageID
                    }.toList()
                }

            runLaterReturn {
                dIm.click()
            }
                """.trimIndent()
            )

            println("clicked")
            unsafeError(
                """
                    val secondViewerImagesAfter: List<Int> =
                runLaterReturn {
                    secondViewer.recurseSelfAndChildNodes<DeephyImView>().map {
                        it.weakIm.deref()!!.imageID
                    }.toList()
                }

            assertNotEquals(secondViewerImagesBefore, secondViewerImagesAfter)

                """.trimIndent()
            )

            clicked++
        }
        if (clicked < NUM_IM_CLICKS) {
            println("sleeping...")
            sleep(WAIT_FOR_GUI_INTERVAL)
            println("slept")
        }
    }

    fun runThroughCategoryView() {
        var clicked = 0
        unsafeError(
            """
            val scene = app.testReadyScene.await()
            val root = scene.root
            val viewer = root.findRecursivelyFirstOrNull<DatasetViewer>()!!
            val cat = viewer.testData.value!!.test.categories.first()
            runLaterReturn {
                viewer.navigateTo(cat)
            }
            /*warn("not animating CategoryPie")
            CategoryPie.ANIMATE = false*/
            val dSetViewsBox = root.findRecursivelyFirstOrNull<DSetViewsVBox>()!!
            val firstViewer = dSetViewsBox.children.first()
            """.trimIndent()
        )
        sleep(WAIT_FOR_GUI_INTERVAL)
        while (clicked < NUM_SLICE_CLICKS) {
            unsafeError(
                """
            val dIm = firstViewer.findRecursivelyFirstOrNull<CategorySlice>()!!    
                """.trimIndent()
            )

            println("clicking a slice...")
            unsafeError(
                """
                    runLaterReturn {
                dIm.click()
            }
                """.trimIndent()
            )

            println("clicked")
            clicked++
            if (clicked < NUM_SLICE_CLICKS) {
                println("sleeping...")
                sleep(WAIT_FOR_GUI_INTERVAL)
                println("slept")
            }
        }
        sleep(WAIT_FOR_GUI_INTERVAL)
        clicked = 0
        while (clicked < NUM_SLICE_CLICKS) {
            unsafeError(
                """
            val dIm = firstViewer.findRecursivelyFirstOrNull<CategorySlice>()!!    
                """.trimIndent()
            )

            println("shift clicking a slice...")
            unsafeError(
                """
                    runLaterReturn {
                dIm.shiftClick()
            }
                """.trimIndent()
            )

            println("shift clicked")
            clicked++
            if (clicked < NUM_SLICE_CLICKS) {
                println("sleeping...")
                sleep(WAIT_FOR_GUI_INTERVAL)
                println("slept")
            }
        }
    }

    fun disposeAllTestsAndCheckMemory() {
        @Suppress("unused")
        val scene = app.testReadyScene.await()
        val threshold = 500.mebibytes
        val postGCWaitSecs = 20
        unsafeError(
            """
            val root = scene.root
            runLaterReturn {
                println("in runLaterReturn to removeAllTests")
                root.findRecursivelyFirstOrNull<DSetViewsVBox>()!!.removeAllTests()
                println("finished runLaterReturn to removeAllTests")
            }
            """.trimIndent()
        )
        println("waiting for delete caches thread...")
        DeephysCacheManager.cacheDeleter.await() /*can hold a significant amount of memory*/
        println("finished waiting for delete caches thread")
        println("sleeping for 1 sec")
        sleep(1.seconds)
        println("running gc for $postGCWaitSecs sec")
        for (it in 0..postGCWaitSecs) {
            /*ahh... finally found a solution. A loop with multiple collections instead of just one collection followed by endless pointless waiting. I best I know what happened: I was doing the gc too early and some things were still strongly reachable for whatever reason deep in some internal libs*/
            @Suppress("ExplicitGarbageCollectionCall")
            RUNTIME.gc()
            sleep(1.seconds)
            val u = MemReport().used
            println("u$it=$u")
            if (u < threshold) {
                println("waking up early because I've gone under the memory Threshold of $threshold. Yay!")
                break
            }
        }
        val u = MemReport().used
        println("uFinal=$u")
        assertTrueLazyMessage(u < threshold) {
            check(profiler.engine == YourKit) {
                "Programmatic JProfiler memory snapshots do not seem to work from tests, which I think are a bit weird in how they fork from the gradle jvm. Yourkit on the other hand, works perfectly. It is also more automated, and deserves more of my attention as it does the same essential things as JProfiler and in many ways seems to do it way more conveniently."
            }
            val _ = profiler.captureMemorySnapshot()
            "test data did not properly dispose. After removing all tests, expected used memory to be less than $threshold, but it is $u"
        }
    }
}
