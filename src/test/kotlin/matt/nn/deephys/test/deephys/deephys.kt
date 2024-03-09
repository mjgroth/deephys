package matt.nn.deephys.test.deephys

import javafx.application.Platform
import kotlinx.coroutines.launch
import matt.async.thread.daemon
import matt.caching.compcache.ComputeInput
import matt.collect.itr.list
import matt.file.commons.desktop.DEEPHYS_RAM_SAMPLES_FOLDER
import matt.file.commons.desktop.RAM_NUMBERED_FILES
import matt.file.commons.reg.DEEPHYS_DATA_FOLDER
import matt.file.toJioFile
import matt.http.http
import matt.json.prim.saveAsJsonTo
import matt.lang.anno.SeeURL
import matt.log.profile.data.RamSample
import matt.log.profile.data.ramSample
import matt.log.profile.real.Profiler
import matt.model.data.rect.DoubleRectSize
import matt.nn.deephys.gui.navbox.zoo.NeuronalActivityZoo
import matt.nn.deephys.test.deephys.tester.DeephysTestSession
import matt.reflect.scan.jcommon.systemScope
import matt.reflect.scan.jcommon.usingClassGraph
import matt.reflect.scan.mattSubClasses
import matt.test.Tests
import matt.test.assertions.assertTrueLazyMessage
import matt.test.co.runTestWithTimeoutOnlyIfTestingPerformance
import matt.test.prop.j.TestPerformance
import matt.time.dur.sleep
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val NUM_IM_CLICKS = if (TestPerformance.get()) 10 else 2
val NUM_SLICE_CLICKS = if (TestPerformance.get()) 10 else 2
val WAIT_FOR_GUI_INTERVAL = 100.milliseconds


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


val tests =
    list {
        add(
            DeephysTestData(
                name = "CIFARX2",
                model = "resnet18_cifar.model",
                tests =
                    listOf(
                        "CIFARV1.test", "CIFARV2.test"
                    ),
                expectedLoadTime = 5.seconds
            )
        )
        if (TestPerformance.get()) {
            add(
                DeephysTestData(
                    name = "INX3",
                    model = "resnet50_imagenet.model",
                    tests =
                        listOf(
                            "ImageNetV1_resnet50.test", "ImageNet_style_resnet50.test", "ImageNet_sketch_resnet50.test"
                        ),
                    expectedLoadTime = 30.seconds
                )
            )
        }
    }


@SeeURL("https://www.theverge.com/2013/7/15/4523668/11-inch-macbook-air-review")
val MAC_MAYBE_MIN_SCREEN_SIZE =
    DoubleRectSize(
        width = 1366.0, height = 768.0
    )

@TestInstance(PER_CLASS)
class TestDeephys(
    profiler: Profiler
) : Tests() {

    val session = DeephysTestSession(profiler)


    companion object {

        private val ramSamples = mutableListOf<RamSample>()


        init {
            DEEPHYS_RAM_SAMPLES_FOLDER.mkdirs()
        }

        private val myRamSamplesJson by lazy {
            RAM_NUMBERED_FILES.nextFile().toJioFile()
        }

        @Synchronized
        fun sampleRam() {
            ramSamples.add(ramSample())
            ramSamples.saveAsJsonTo(myRamSamplesJson, false)
        }


        private var getRamSamples = true

        @JvmStatic
        @BeforeAll
        fun startSamplingRam() {
            daemon("startSamplingRam Thread") {
                while (getRamSamples) {
                    sampleRam()
                    sleep(500.milliseconds)
                }
            }
        }

        @JvmStatic
        @AfterAll
        fun stopSamplingRam() {
            getRamSamples = false
        }

        @JvmStatic
        @AfterAll
        fun shutdownJavaFX() {
            Platform.exit()
        }
    }


    @Test
    fun computeInputsAreData() =
        with(systemScope(includePlatformClassloader = false).usingClassGraph()) {
            ComputeInput::class.mattSubClasses().forEach {
                assertTrueLazyMessage(it.isData || it.isAbstract) {
                    "$it is a ComputeInput but not data... how is it supposed to cache stuff correctly?"
                }
            }
        }


    @Test
    fun correctTitle() = session.testHasCorrectTitle()


    @Test
    fun fitsInSmallestScreen() = session.testFitsInSmallestScreen()

    @TestMethodOrder(OrderAnnotation::class)
    @Nested
    inner class TestRunningApp {

        @Test
        @Order(1)
        fun loadDataRunThroughFeaturesAndDispose() {

            tests.forEach {
                session.loadDataAndCheckItWasFastEnough(
                    key = it.name, testData = it, maxTime = it.expectedLoadTime
                )
                session.runThroughByImageView()
                session.runThroughCategoryView()
                session.disposeAllTestsAndCheckMemory()
            }
        }
    }

    @Test fun downloadZooUrls() {
        runTestWithTimeoutOnlyIfTestingPerformance {
            val example = NeuronalActivityZoo.EXAMPLES.first()
            launch {
                http(example.modelURL).requireSuccessful()
            }
            http(example.testURLs.first()).requireSuccessful()
        }
    }
}

