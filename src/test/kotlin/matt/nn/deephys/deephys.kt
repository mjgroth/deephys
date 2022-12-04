package matt.nn.deephys

import javafx.application.Platform
import javafx.application.Platform.runLater
import javafx.scene.control.Alert.AlertType.CONFIRMATION
import javafx.scene.control.ButtonType
import matt.async.thread.daemon
import matt.caching.compcache.GlobalRAMComputeInput
import matt.collect.itr.list
import matt.collect.itr.recurse.recurse
import matt.file.CborFile
import matt.file.commons.DEEPHYS_DATA_FOLDER
import matt.file.commons.DEEPHYS_RAM_SAMPLES_FOLDER
import matt.file.commons.DEEPHYS_TEST_RESULT_JSON
import matt.file.commons.RAM_NUMBERED_FILES
import matt.file.toSFile
import matt.fx.control.tfx.dialog.asyncAlert
import matt.fx.graphics.fxthread.runLaterReturn
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.json.prim.loadJson
import matt.json.prim.save
import matt.json.prim.saveAsJsonTo
import matt.lang.anno.SeeURL
import matt.log.profile.data.RamSample
import matt.log.profile.data.TestResults
import matt.log.profile.data.TestSession
import matt.log.profile.data.ramSample
import matt.log.profile.mem.MemReport
import matt.log.profile.stopwatch.tic
import matt.model.data.byte.megabytes
import matt.model.data.rect.RectSize
import matt.nn.deephys.gui.Arg.`erase-state`
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.state.DeephyState
import matt.obs.subscribe.waitForThereToBeAtLeastOneNotificationThenUnsubscribe
import matt.reflect.reflections.mattSubClasses
import matt.test.assertTrueLazyMessage
import matt.time.dur.sleep
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val TEST_DATA_FOLDER = DEEPHYS_DATA_FOLDER["test"]

class DeephysTestData(
  val name: String,
  model: String,
  tests: List<String>,
  val expectedLoadTime: Duration
) {
  private val root = TEST_DATA_FOLDER[name]
  val model = root[model]
  val tests = tests.map { root[it] }
}

val tests = list {
  add(
	DeephysTestData(
	  name = "CIFARX2",
	  model = "resnet18_cifar.model",
	  tests = listOf(
		"CIFARV1.test",
		"CIFARV2.test"
	  ),
	  expectedLoadTime = 15.seconds
	)
  )
  //  disabledCode {
  add(
	DeephysTestData(
	  name = "INX3",
	  model = "resnet50_imagenet.model",
	  tests = listOf(
		"ImageNetV1_resnet50.test",
		"ImageNet_style_resnet50.test",
		"ImageNet_sketch_resnet50.test"
	  ),
	  expectedLoadTime = 2.minutes
	)
  )
  //  }
}


@SeeURL("https://www.theverge.com/2013/7/15/4523668/11-inch-macbook-air-review")    /*@TestClassOrder()*/
val MAC_MAYBE_MIN_SCREEN_SIZE = RectSize(
  width = 1366.0,
  height = 768.0
)

@TestInstance(PER_CLASS) class TestDeephys {

  val app by lazy {
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


  companion object {

	private val ramSamples = mutableListOf<RamSample>()

	init {
	  DEEPHYS_RAM_SAMPLES_FOLDER.mkdirs()
	}

	private val myRamSamplesJson by lazy {
	  RAM_NUMBERED_FILES.nextFile()
	}


	@Synchronized
	fun sampleRam() {
	  ramSamples.add(ramSample())
	  ramSamples.saveAsJsonTo(myRamSamplesJson, false)
	}


	private var getRamSamples = true
	@JvmStatic @BeforeAll fun startSamplingRam() {
	  daemon {
		while (getRamSamples) {
		  sampleRam()
		  sleep(500.milliseconds)
		}
	  }
	}

	@JvmStatic @AfterAll fun stopSamplingRam() {
	  getRamSamples = false
	}

	@JvmStatic @AfterAll fun shutdownJavaFX() {
	  Platform.exit()
	}


  }


  @Test fun computeInputsAreData() {
	GlobalRAMComputeInput::class.mattSubClasses().forEach {
	  assertTrueLazyMessage(it.isData || it.isAbstract) {
		"$it is a ComputeInput but not data... how is it supposed to cache stuff correctly?"
	  }
	}
  }


  @Test fun correctTitle() = assertEquals(
	expected = app.stageTitle.await(), actual = mainStage.title
  )


  @Test fun fitsInSmallestScreen() {
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

  @TestMethodOrder(OrderAnnotation::class) @Nested inner class TestRunningApp {

	@Order(1)
	@Test
	fun fastLoad() {
	  tests.forEach {
		loadData(
		  it.name,
		  it,
		  maxTime = it.expectedLoadTime
		)
	  }
	}


	private fun loadData(
	  key: String,
	  testData: DeephysTestData,
	  maxTime: Duration
	) {
	  sampleRam()
	  val t = tic("runThroughFeatures")
	  fun tocAndSampleRam(marker: String): Duration? {
		val r = t.toc(marker)
		sampleRam()
		return r
	  }

	  val scene = app.testReadyScene.await()
	  tocAndSampleRam("got scene")
	  val root = scene.root

	  val sub = app.testReadyDSetViewsBbox.subscribe()
	  runLater {
		(root.recurse<NW> { (it as? RegionWrapper<*>)?.children ?: listOf() }.firstOrNull {
		  it is DSetViewsVBox
		} as? DSetViewsVBox)?.removeAllTests()
		DeephyState.model.value = testData.model.toSFile()
	  }
	  sub.waitForThereToBeAtLeastOneNotificationThenUnsubscribe()
	  tocAndSampleRam("GUI ready")


	  val dSetViewsBox = root.recurse<NW> { (it as? RegionWrapper<*>)?.children ?: listOf() }.first {
		it is DSetViewsVBox
	  } as DSetViewsVBox
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
		dSetViewsBox.bound.value = firstViewer
	  }
	  val totalTime = tocAndSampleRam("set binding")!!



	  mySession.tests.add(
		TestResults(
		  name = key,
		  loadMillis = totalTime.inWholeMilliseconds
		)
	  )
	  DEEPHYS_TEST_RESULT_JSON.save(sessionList, pretty = true)

	  assertTrueLazyMessage(
		totalTime < maxTime
	  ) {
		"took to long to load: $totalTime"
	  }

	  sampleRam()

	}

	@Test @Order(2) fun runThroughFeatures() {
	  val response = runLaterReturn {
		println("opening async alert")
		asyncAlert(
		  CONFIRMATION, "Manually Test Binding",
		  "If I click images on the top, does the dataset on the bottom correctly follow?", ButtonType.NO,
		  ButtonType.YES, owner = mainStage
		) {
		  runLater {
			x = mainStage.x + (mainStage.width/2.0) - (width/2.0)
			y = mainStage.y - height
		  }

		}
	  }

	  println("waiting for response")

	  response.join {
		if (it != ButtonType.YES) fail("it was bad")
	  }

	}

	@Test @Order(3) fun testsAreDisposed() {
	  val scene = app.testReadyScene.await()
	  val root = scene.root
	  runLater {
		(root.recurse<NW> { (it as? RegionWrapper<*>)?.children ?: listOf() }.firstOrNull {
		  it is DSetViewsVBox
		} as? DSetViewsVBox)?.removeAllTests()
	  }
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
		"test data did not properly dispose. After removing all tests, expected used memory to be less than $threshold, but it is ${u}"
	  }
	}

  }


}

