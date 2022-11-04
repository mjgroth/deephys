package matt.nn.deephys

import javafx.stage.Stage
import javafx.stage.Window
import matt.collect.itr.recurse.recurse
import matt.file.CborFile
import matt.file.commons.DEEPHYS_DATA_FOLDER
import matt.file.toSFile
import matt.fx.control.tfx.dialog.confirm
import matt.fx.control.wrapper.wrapped.wrapped
import matt.fx.graphics.fxthread.runLaterReturn
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.fx.graphics.wrapper.stage.StageWrapper
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.stageTitle
import matt.nn.deephys.state.DeephyState
import matt.time.dur.sleep
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
	fun openApp(): Unit {
	  thread {
		main(arrayOf())
	  }
	  while (mainStage == null) {
		mainStage = (Window.getWindows().firstOrNull { it.isShowing } as? Stage)?.wrapped()
		sleep(100.milliseconds)
	  }
	}
  }

  @Test
  fun correctTitle() = assertEquals(
	expected = stageTitle.await(),
	actual = mainStage!!.title
  )


  @Test
  fun runThroughFeatures() {

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



	val (testViewer1, _) = runLaterReturn {
	  val testViewer1 = dSetViewsBox.addTest()
	  val testViewer2 = dSetViewsBox.addTest()

	  testViewer1.file.value = CborFile(TEST_FOLDER["CIFARV1.test"].abspath)
	  testViewer2.file.value = CborFile(TEST_FOLDER["CIFARV2.test"].abspath)

	  testViewer1 to testViewer2
	}


	runLaterReturn {
	  testViewer1.layerSelection.value = dSetViewsBox.model.resolvedLayers.first().interTest
	}


	val good = runLaterReturn {
	  var r = false
	  confirm("does it look good?", owner = mainStage!!) {
		r = true
	  }
	  r
	}

	if (!good) fail("it was bad")

	//	println("does it look good?")
	//	runInputLoop {
	//	  terminatingCommand("y", desc = "it looks good")
	//	  terminatingCommand("n", desc = "it looks bad") {
	//		fail("it looked bad")
	//	  }
	//	}


  }


}