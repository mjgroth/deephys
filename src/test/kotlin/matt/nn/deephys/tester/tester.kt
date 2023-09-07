package matt.nn.deephys.tester

import javafx.application.Platform
import matt.async.thread.namedThread
import matt.file.CborFile
import matt.file.commons.DEEPHYS_TEST_RESULT_JSON
import matt.file.toSFile
import matt.fx.graphics.fxthread.RunLaterReturnLatchManager
import matt.fx.graphics.fxthread.runLaterReturn
import matt.fx.graphics.wrapper.node.findRecursivelyFirstOrNull
import matt.fx.graphics.wrapper.node.recurseSelfAndChildNodes
import matt.gui.service.AsyncFXActionAbilitiesService
import matt.json.prim.loadJson
import matt.json.prim.saveJson
import matt.lang.anno.optin.ExperimentalMattCode
import matt.lang.profiling.IsProfilingWithJProfiler
import matt.lang.profiling.IsProfilingWithYourKit
import matt.log.profile.data.TestResults
import matt.log.profile.data.TestSession
import matt.log.profile.jp.JProfiler
import matt.log.profile.real.Profiler
import matt.log.profile.stopwatch.tic
import matt.log.profile.yk.YourKit
import matt.log.report.MemReport
import matt.model.code.errreport.reportAndReThrowErrorsBetter
import matt.model.data.byte.mebibytes
import matt.nn.deephys.DeephysTestData
import matt.nn.deephys.MAC_MAYBE_MIN_SCREEN_SIZE
import matt.nn.deephys.NUM_IM_CLICKS
import matt.nn.deephys.NUM_SLICE_CLICKS
import matt.nn.deephys.TestDeephys
import matt.nn.deephys.WAIT_FOR_GUI_INTERVAL
import matt.nn.deephys.gui.DeephysArg.reset
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.category.pie.CategoryPie.CategorySlice
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.settings.DeephySettingsNode
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.cache.DeephysCacheManager
import matt.nn.deephys.state.DeephyState
import matt.obs.subscribe.waitForThereToBeAtLeastOneNotificationThenUnsubscribe
import matt.prim.str.elementsToString
import matt.test.assertTrueLazyMessage
import matt.test.prop.ManualTests
import matt.test.prop.TestPerformance
import matt.time.dur.sleep
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMattCode::class)
class DeephysTestSession {

    private val profiler by lazy {
        val engine = when {
            IsProfilingWithYourKit -> YourKit
            IsProfilingWithJProfiler -> JProfiler(snapshotFolder = JProfiler.defaultSnapshotFolder())
            else -> null
        }

        Profiler(
            engine = engine ?: YourKit,
            enableAll = engine != null
        ) {
            engine!!.openSnapshot(it)
        }
    }

    private val app by lazy {
        DeephysApp()
    }


    private val mainStage by lazy {
        app.readyForConfiguringWindowFromTest.await()
    }


    init {
        reportAndReThrowErrorsBetter {
            val settingsNode = DeephySettingsNode()
            app.boot2(settingsNode = settingsNode, listOf(reset)) /*need this so tests are deterministic*/
            namedThread(name = "App Launcher") {
                try {
                    app.boot2(args = listOf(), settingsNode = settingsNode, throwOnApplicationThreadThrowable = true)
                } catch (e: Throwable) {
                    println("CANCELLING ALL LATCHES")
                    app.cancelAllLatches(e)
                }
            }
            val theMainStage = mainStage
            runLaterReturn {
                theMainStage.width = MAC_MAYBE_MIN_SCREEN_SIZE.width
                theMainStage.height = MAC_MAYBE_MIN_SCREEN_SIZE.height
                theMainStage.centerOnScreen()
            }
        }
    }

    private val confirmService by lazy {
        AsyncFXActionAbilitiesService(mainStage)
    }

    fun testConfirmation(
        prompt: String,
        force: Boolean = false
    ) =
        if (force || ManualTests.get()) matt.test.testConfirmation(prompt, confirmService) else Unit


    fun testHasCorrectTitle() = assertEquals(
        expected = app.stageTitle.await(), actual = mainStage.title
    )

    fun testFitsInSmallestScreen() {
        val w = mainStage.width
        val h = mainStage.height
        assertTrueLazyMessage(
            w == MAC_MAYBE_MIN_SCREEN_SIZE.width && h == MAC_MAYBE_MIN_SCREEN_SIZE.height
        ) {
            "mainStage .width=$w .height= $h}"
        }
    }

    init {
        DEEPHYS_TEST_RESULT_JSON.mkparents()
    }


    val sessionList = if (DEEPHYS_TEST_RESULT_JSON.doesNotExist || DEEPHYS_TEST_RESULT_JSON.text.isBlank()) {
        mutableListOf<TestSession>()
    } else {
        DEEPHYS_TEST_RESULT_JSON.loadJson()
    }

    val mySession = TestSession().also { sessionList.add(it) }

    fun loadDataAndCheckItWasFastEnough(
        key: String,
        testData: DeephysTestData,
        maxTime: Duration
    ) {
        TestDeephys.sampleRam()
        val t = tic("runThroughFeatures")
        fun tocAndSampleRam(marker: String): Duration? {
            val r = t.toc(marker)
            TestDeephys.sampleRam()
            return r
        }

        val scene = app.testReadyScene.await()
        tocAndSampleRam("got scene")
        val root = scene.root
        val sub = app.testReadyDSetViewsBbox.subscribe()
        profiler.recordCPU {
            Platform.runLater {
                root.findRecursivelyFirstOrNull<DSetViewsVBox>()?.removeAllTests()
                DeephyState.model.value = testData.model.toSFile()
            }
            sub.waitForThereToBeAtLeastOneNotificationThenUnsubscribe(RunLaterReturnLatchManager)
            tocAndSampleRam("GUI ready")


            val dSetViewsBox = root.findRecursivelyFirstOrNull<DSetViewsVBox>()!!
            tocAndSampleRam("found dSetViewsBox")


            val testViewersAndFiles = runLaterReturn {
                testData.tests.map {
                    dSetViewsBox.addTest() to it
                }
            }
            tocAndSampleRam("added tests")

            runLaterReturn {
                testViewersAndFiles.forEach {
                    it.first.file.value = CborFile(it.second.abspath)
                }
            }
            tocAndSampleRam("set test files")

            testViewersAndFiles.forEachIndexed { index, it ->
                it.first.testData.value!!.awaitFinishedTest()
                tocAndSampleRam("test ${index + 1} finished loading")
            }

            val firstViewer = testViewersAndFiles.first().first

            runLaterReturn {
                firstViewer.layerSelection.value = dSetViewsBox.model.resolvedLayers.first().interTest
            }
            tocAndSampleRam("selected layer")

            runLaterReturn {
                dSetViewsBox.selectViewerToBind(firstViewer, makeInDToo = true)
            }
        }
        val totalTime = tocAndSampleRam("set binding")!!
        mySession.tests.add(
            TestResults(
                name = key, loadMillis = totalTime.inWholeMilliseconds
            )
        )
        DEEPHYS_TEST_RESULT_JSON.saveJson(sessionList, pretty = true)
        assertTrueLazyMessage(
            !TestPerformance.get()
                    || totalTime < maxTime
        ) {
            "took to long to load: took=$totalTime expected=$maxTime"
        }
        TestDeephys.sampleRam()
    }

    fun runThroughByImageView() {

//        testConfirmation(
//            prompt = "If I click images on the top, does the dataset on the bottom correctly follow?"
//        )


        println("awaiting scene to be ready...")
        val scene = app.testReadyScene.await()
        println("automatically clicking through $NUM_IM_CLICKS images")
        val root = scene.root
        val dSetViewsBox = root.findRecursivelyFirstOrNull<DSetViewsVBox>()!!
        val viewers = dSetViewsBox.children
        val firstViewer = viewers.first()
        val secondViewer = viewers[1]
        runLaterReturn {
            firstViewer.navigateTo(firstViewer.testData.value!!.awaitImage(0))
        }
        sleep(WAIT_FOR_GUI_INTERVAL)
        var clicked = 0
        while (clicked < NUM_IM_CLICKS) {
            val firstViewerSelection = firstViewer.imageSelection.value
            val dIm = firstViewer.recurseSelfAndChildNodes<DeephyImView>().firstOrNull {
                val im = it.weakIm.deref()!!
                im != firstViewerSelection
            } ?: run {
                val imViews = firstViewer.recurseSelfAndChildNodes<DeephyImView>().toList()
                error(
                    "could not find an image different from $firstViewerSelection, all=${
                        imViews.map { it.weakIm.deref()?.imageID }
                            .elementsToString()
                    }, imViews=${imViews.size}"
                )
            }
            println("clicking an image...")

            val secondViewerImagesBefore: List<Int> = runLaterReturn {
                secondViewer.recurseSelfAndChildNodes<DeephyImView>().map {
                    it.weakIm.deref()!!.imageID
                }.toList()
            }

            runLaterReturn {
                dIm.click()
            }

            println("clicked")

            val secondViewerImagesAfter: List<Int> = runLaterReturn {
                secondViewer.recurseSelfAndChildNodes<DeephyImView>().map {
                    it.weakIm.deref()!!.imageID
                }.toList()
            }

            assertNotEquals(secondViewerImagesBefore, secondViewerImagesAfter)

            clicked++

            if (clicked < NUM_IM_CLICKS) {
                println("sleeping...")
                sleep(WAIT_FOR_GUI_INTERVAL)
                println("slept")
            }
        }
    }

    fun runThroughCategoryView() {
        val scene = app.testReadyScene.await()
        val root = scene.root
        val viewer = root.findRecursivelyFirstOrNull<DatasetViewer>()!!
        val cat = viewer.testData.value!!.test.categories.first()
        runLaterReturn {
            viewer.navigateTo(cat)
        }
        /*warn("not animating CategoryPie")
        CategoryPie.ANIMATE = false*/
//        testConfirmation("click and shift-click around different categories. Does the ByCategoryView look ok?")
        val dSetViewsBox = root.findRecursivelyFirstOrNull<DSetViewsVBox>()!!
        val firstViewer = dSetViewsBox.children.first()
        sleep(WAIT_FOR_GUI_INTERVAL)
        var clicked = 0
        while (clicked < NUM_SLICE_CLICKS) {
            //		if (clicked == 3) {
            //		  YourKit.captureAndOpenMemorySnapshot()
            //		  testConfirmation("done checking memory snapshot", force = true)
            //		}
            val dIm = firstViewer.findRecursivelyFirstOrNull<CategorySlice>()!!
            println("clicking a slice...")
            runLaterReturn {
                dIm.click()
            }
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
            val dIm = firstViewer.findRecursivelyFirstOrNull<CategorySlice>()!!
            println("shift clicking a slice...")
            runLaterReturn {
                dIm.shiftClick()
            }
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
        val scene = app.testReadyScene.await()
        val root = scene.root
        runLaterReturn {
            println("in runLaterReturn to removeAllTests")
            root.findRecursivelyFirstOrNull<DSetViewsVBox>()!!.removeAllTests()
            println("finished runLaterReturn to removeAllTests")
        }
        println("waiting for delete caches thread...")
        DeephysCacheManager.cacheDeleter.await() /*can hold a significant amount of memory*/
        println("finished waiting for delete caches thread")
        //	println("sleeping forever 1")
        //	sleep(1.days)
        println("sleeping for 1 sec")
        sleep(1.seconds)
        val postGCWaitSecs = 20
        println("running gc for $postGCWaitSecs sec")
        val threshold = 500.mebibytes

        for (it in 0..postGCWaitSecs) {
            /*ahh... finally found a solution. A loop with multiple collections instead of just one collections followed by endless pointless waiting. I best I know what happened: I was doing the gc too early and some things were still strongly reachable for whatever reasons deep in some internal libs*/
            Runtime.getRuntime().gc()
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
            //	  println("sleeping forever 2")
            //	  sleep(1.days)
            profiler.captureMemorySnapshot()
            "test data did not properly dispose. After removing all tests, expected used memory to be less than $threshold, but it is $u"
        }
    }
}

