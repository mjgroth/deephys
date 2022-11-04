package matt.nn.deephys

import javafx.application.Platform
import javafx.application.Platform.runLater
import javafx.scene.control.Alert.AlertType.CONFIRMATION
import javafx.scene.control.ButtonType
import javafx.stage.Stage
import javafx.stage.Window
import matt.collect.itr.recurse.recurse
import matt.file.CborFile
import matt.file.commons.DEEPHYS_DATA_FOLDER
import matt.file.toSFile
import matt.fx.control.tfx.dialog.asyncAlert
import matt.fx.control.wrapper.wrapped.wrapped
import matt.fx.graphics.fxthread.runLaterReturn
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.fx.graphics.wrapper.stage.StageWrapper
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.stageTitle
import matt.nn.deephys.state.DeephyState
import matt.test.yesIUseTestLibs
import matt.time.dur.sleep
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds


class SomeTests {

  companion object {

	private var mainStage: StageWrapper? = null

	@JvmStatic
	@BeforeAll
	fun openApp() {


	  thread {
		main(arrayOf())
	  }
	  while (mainStage == null) {
		mainStage = (Window.getWindows().firstOrNull { it.isShowing } as? Stage)?.wrapped()
		sleep(100.milliseconds)
	  }
	}


	@JvmStatic
	@AfterAll
	fun shutdownJavaFX() {
	  Platform.exit()
	}


  }

  @Test
  fun correctTitle() = assertEquals(
	expected = stageTitle.await(),
	actual = mainStage!!.title
  )


  @Test
  fun runThroughFeatures() {

	yesIUseTestLibs()

	val scene = mainStage!!.scene!!


	val root = scene.root


	val TEST_FOLDER = DEEPHYS_DATA_FOLDER["test"]

	runLaterReturn {
	  DeephyState.tests.value = null
	  DeephyState.model.value = TEST_FOLDER["resnet18_cifar.model"].toSFile()
	}

	/*this needs to be after selecting the model above*/
	sleep(2000.milliseconds) /*give the DSetViewsVBox some time to be added*/
	val dSetViewsBox = root.recurse<NW> { (it as? RegionWrapper<*>)?.children ?: listOf() }.first {
	  it is DSetViewsVBox
	} as DSetViewsVBox

	sleep(100.milliseconds) /*give UI time to update*/


	val (testViewer1, testViewer2) = runLaterReturn {
	  val testViewer1 = dSetViewsBox.addTest()
	  val testViewer2 = dSetViewsBox.addTest()
	  testViewer1 to testViewer2


	}

	sleep(100.milliseconds) /*give UI time to update*/

	runLaterReturn {
	  testViewer1.file.value = CborFile(TEST_FOLDER["CIFARV1.test"].abspath)
	  testViewer2.file.value = CborFile(TEST_FOLDER["CIFARV2.test"].abspath)
	}

	while (testViewer1.testData.value!!.progress.value < 1.0) {
	  sleep(100.milliseconds) /*wait for load*/
	}

	while (testViewer2.testData.value!!.progress.value < 1.0) {
	  sleep(100.milliseconds) /*wait for load*/
	}

	sleep(100.milliseconds) /*give UI time to update*/

	runLaterReturn {
	  testViewer1.layerSelection.value = dSetViewsBox.model.resolvedLayers.first().interTest
	}

	sleep(100.milliseconds) /*give UI time to update*/

	runLaterReturn {
	  dSetViewsBox.bound.value = testViewer1
	}

	sleep(100.milliseconds) /*give UI time to update*/

	val response = runLaterReturn {
	  println("opening async alert")
	  asyncAlert(
		CONFIRMATION, "Manually Test Binding",
		"If I click images on the top, does the dataset on the bottom correctly follow?", ButtonType.NO, ButtonType.YES,
		owner = mainStage!!
	  ) {
		runLater {
		  x = mainStage!!.x + (mainStage!!.width/2.0) - (width/2.0)
		  y = mainStage!!.y - height
		}

	  }
	}

	println("waiting for response")

	response.join {
	  if (it != ButtonType.YES) fail("it was bad")
	}

  }


}