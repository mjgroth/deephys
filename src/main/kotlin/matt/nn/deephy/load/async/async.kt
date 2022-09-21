package matt.nn.deephy.load.async

import javafx.application.Platform.runLater
import matt.file.CborFile
import matt.model.latch.SimpleLatch
import matt.obs.bindings.bool.ObsB
import matt.obs.prop.BindableProperty

abstract class AsyncLoader(file: CborFile) {
  val fileFound: ObsB = BindableProperty(file.exists())
  val streamOk: ObsB = BindableProperty(true)
  val parseError: ObsB = BindableProperty(false)
  val finishedLoading: ObsB = BindableProperty(false)
  protected val finishedLoadingLatch = SimpleLatch()
  protected fun signalStreamNotOk() {
	runLater {
	  (streamOk as BindableProperty).value = false
	}
  }

  protected fun signalParseError() {
	runLater {
	  (parseError as BindableProperty).value = true
	}
  }

  protected fun signalFinishedLoading() {
	finishedLoadingLatch.open()
	runLater {
	  (finishedLoading as BindableProperty).value = true
	}
  }
}