package matt.nn.deephys.load.async

import javafx.application.Platform.runLater
import matt.cbor.err.CborParseException
import matt.file.CborFile
import matt.model.await.Awaitable
import matt.obs.bindings.bool.ObsB
import matt.obs.prop.BindableProperty

abstract class AsyncLoader(file: CborFile) {
  val fileFound: ObsB = BindableProperty(file.exists())
  val streamOk: ObsB = BindableProperty(true)
  val parseError = BindableProperty<CborParseException?>(null)
  val finishedLoading: ObsB = BindableProperty(false)
  abstract val finishedLoadingAwaitable: Awaitable<*>

  protected fun signalFileNotFound() {
	runLater {
	  (fileFound as BindableProperty).value = false
	}
  }

  protected fun signalStreamNotOk() {
	runLater {
	  (streamOk as BindableProperty).value = false
	}
  }


  protected fun signalParseError(e: CborParseException) {
	runLater {
	  parseError.value = e
	}
  }

  protected fun signalFinishedLoading() {
	/*finishedLoadingLatch.open()*/
	runLater {
	  (finishedLoading as BindableProperty).value = true
	}
  }
}