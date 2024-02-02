package matt.nn.deephys.load.async

import javafx.application.Platform.runLater
import matt.file.toJioFile
import matt.lang.idea.FailableIdea
import matt.lang.model.file.types.Cbor
import matt.lang.model.file.types.TypedFile
import matt.model.flowlogic.await.ThreadAwaitable
import matt.model.flowlogic.latch.asyncloaded.Async
import matt.obs.bindings.bool.ObsB
import matt.obs.prop.BindableProperty

abstract class AsyncLoader(private val file: TypedFile<Cbor,*>) {
    val fileFound: ObsB = BindableProperty(file.toJioFile().exists())
    val streamOk: ObsB = BindableProperty(true)
    val parseError = BindableProperty<Exception?>(null)
    val finishedLoading: ObsB = BindableProperty(false)
    abstract val finishedLoadingAwaitable: ThreadAwaitable<*>

    protected fun signalFileNotFound() {
        failableValueSlots.forEach {
            it.admitFailureIfNotDone("File not found: $file")
        }
        runLater {
            (fileFound as BindableProperty).value = false
        }
    }

    protected fun signalStreamNotOk() {
        failableValueSlots.forEach {
            it.admitFailureIfNotDone("Stream Not Ok")
        }
        runLater {
            (streamOk as BindableProperty).value = false
        }
    }


    protected fun signalParseError(e: Exception) {
        failableValueSlots.forEach {
            it.admitFailureIfNotDone(e.toString())
        }
        runLater {
            parseError.value = e
        }
    }

    protected fun signalFinishedLoading() {
        runLater {
            (finishedLoading as BindableProperty).value = true
        }
    }


    sealed interface LoadedOrFailed<T> : FailableIdea

    @JvmInline
    value class Loaded<T>(val value: T) : LoadedOrFailed<T>
    class Failed<T>(val message: String) : LoadedOrFailed<T> {
        override fun toString(): String = "Failed: $message"
    }


    private val failableValueSlots = mutableListOf<LoadedOrFailedValueSlot<*>>()

    inner class LoadedOrFailedValueSlot<T> : Async<LoadedOrFailed<T>>() {


        init {
            failableValueSlots += this
        }


        @Synchronized
        fun putLoadedValue(t: T) {
            require(latch!!.isClosed)
            value = Loaded(t)
            openAndDisposeLatch()
        }


        fun admitFailureIfNotDone(message: String) {
            if (!isDone()) {
                require(latch!!.isClosed)
                value = Failed(message)
                openAndDisposeLatch()
            }
        }

        fun isDone() = latch?.isOpen ?: true

        fun awaitRequireSuccessful() = (await() as Loaded).value
        fun awaitSuccessfulOrNull() = (await() as? Loaded)?.value
        fun awaitSuccessfulOrMessage() = await().let { (it as? Loaded)?.value ?: (it as Failed).message }


    }


}
