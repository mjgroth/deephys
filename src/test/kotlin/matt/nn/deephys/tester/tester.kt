package matt.nn.deephys.tester

import javafx.application.Platform
import matt.file.CborFile
import matt.file.commons.DEEPHYS_TEST_RESULT_JSON
import matt.file.toSFile
import matt.fx.control.AsyncFXActionAbilitiesService
import matt.fx.graphics.fxthread.runLaterReturn
import matt.fx.graphics.wrapper.node.findRecursivelyFirstOrNull
import matt.fx.graphics.wrapper.node.recurseSelfAndChildNodes
import matt.json.prim.loadJson
import matt.json.prim.save
import matt.log.profile.data.TestResults
import matt.log.profile.data.TestSession
import matt.log.profile.mem.MemReport
import matt.log.profile.stopwatch.tic
import matt.log.profile.yk.YourKit
import matt.model.data.byte.megabytes
import matt.nn.deephys.DeephysTestData
import matt.nn.deephys.MAC_MAYBE_MIN_SCREEN_SIZE
import matt.nn.deephys.NUM_IM_CLICKS
import matt.nn.deephys.NUM_SLICE_CLICKS
import matt.nn.deephys.TestDeephys
import matt.nn.deephys.WAIT_FOR_GUI_INTERVAL
import matt.nn.deephys.gui.Arg.`erase-state`
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.category.pie.CategoryPie.CategorySlice
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.cache.DeephysCacheManager
import matt.nn.deephys.state.DeephyState
import matt.obs.subscribe.waitForThereToBeAtLeastOneNotificationThenUnsubscribe
import matt.test.SHOULD_DO_MANUAL_TESTS
import matt.test.assertTrueLazyMessage
import matt.time.dur.sleep
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val ENABLE_CPU_PROFILING = false

class DeephysTestSession {
  private val app by lazy {
	DeephysApp()
  }


  private val mainStage by lazy {
	app.readyForConfiguringWindowFromTest.await()
  }

  init {
	app.boot(`erase-state`)
	thread {
	  app.boot(arrayOf())
	}
	mainStage.apply {
	  runLaterReturn {
		width = MAC_MAYBE_MIN_SCREEN_SIZE.width
		height = MAC_MAYBE_MIN_SCREEN_SIZE.height
		centerOnScreen()
	  }
	}
  }

  private val confirmService by lazy {
	AsyncFXActionAbilitiesService(mainStage)
  }

  fun testConfirmation(prompt: String, force: Boolean = false) =
	if (force || SHOULD_DO_MANUAL_TESTS) matt.test.testConfirmation(prompt, confirmService) else Unit


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
	YourKit.recordCPU(enable = ENABLE_CPU_PROFILING) {
	  Platform.runLater {
		root.findRecursivelyFirstOrNull<DSetViewsVBox>()?.removeAllTests()
		DeephyState.model.value = testData.model.toSFile()
	  }
	  sub.waitForThereToBeAtLeastOneNotificationThenUnsubscribe()
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
		dSetViewsBox.myToggleGroup.selectedValue.value = firstViewer
	  }
	}
	val totalTime = tocAndSampleRam("set binding")!!
	mySession.tests.add(
	  TestResults(
		name = key, loadMillis = totalTime.inWholeMilliseconds
	  )
	)
	DEEPHYS_TEST_RESULT_JSON.save(sessionList, pretty = true)
	assertTrueLazyMessage(
	  totalTime < maxTime
	) {
	  "took to long to load: $totalTime"
	}
	TestDeephys.sampleRam()
  }

  fun runThroughByImageView() {

	testConfirmation(
	  prompt = "If I click images on the top, does the dataset on the bottom correctly follow?"
	)

	println("automatically clicking through $NUM_IM_CLICKS images")
	val scene = app.testReadyScene.await()
	val root = scene.root
	val dSetViewsBox = root.findRecursivelyFirstOrNull<DSetViewsVBox>()!!
	val firstViewer = dSetViewsBox.children.first()
	runLaterReturn {
	  firstViewer.navigateTo(firstViewer.testData.value!!.awaitImage(0))
	}
	sleep(WAIT_FOR_GUI_INTERVAL)
	var clicked = 0
	while (clicked < NUM_IM_CLICKS) {
	  val dIm = firstViewer.recurseSelfAndChildNodes<DeephyImView>().first {
		val im = it.weakIm.deref()!!
		im != firstViewer.imageSelection.value
	  }
	  println("clicking an image...")
	  runLaterReturn {
		dIm.click()
	  }
	  println("clicked")
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
	testConfirmation("click and shift-click around different categories. Does the ByCategoryView look ok?")
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
	Platform.runLater {
	  root.findRecursivelyFirstOrNull<DSetViewsVBox>()?.removeAllTests()
	}
	println("waiting for delete caches thread...")
	DeephysCacheManager.cacheDeleter.await() /*can hold a significant amount of memory*/
	println("finished waiting for delete caches thread")
	println("sleeping for 1 sec")
	sleep(1.seconds)
	println("running gc")
	Runtime.getRuntime().gc()
	println("sleeping for another sec")
	sleep(1.seconds)
	val threshold = 500.megabytes
	val u = MemReport().used
	println("u=$u")
	assertTrueLazyMessage(u < threshold) {
	  YourKit.captureAndOpenMemorySnapshot()
	  "test data did not properly dispose. After removing all tests, expected used memory to be less than $threshold, but it is $u"
	}
  }
}
