@file:Suppress("NoDuplicatedTypeNames")

package matt.nn.deephys.load.async

import matt.file.JioFile
import matt.file.construct.toJioFile
import matt.lang.anno.Open
import matt.lang.anno.optin.ShadowsExtensionBug
import matt.lang.sync.common.SimpleReferenceMonitor
import matt.lang.sync.common.withLock
import matt.model.flowlogic.await.ThreadAwaitable
import matt.model.flowlogic.latch.asyncloaded.Async
import matt.obs.bindings.bool.ObsB
import matt.obs.prop.writable.BindableProperty
import matt.prim.exportfromlang.ideas.SupertypeIdea
import matt.prim.exportfromlang.ideas.SupertypeIdeaType

abstract class AsyncLoader(private val cborFile: JioFile) {
    @OptIn(ShadowsExtensionBug::class)
    val fileFound: ObsB = BindableProperty(cborFile.toJioFile().exists())
    val streamOk: ObsB = BindableProperty(true)
    val parseError = BindableProperty<Exception?>(null)
    private val finishedLoading: ObsB = BindableProperty(false)
    abstract val finishedLoadingAwaitable: ThreadAwaitable<*>

    protected fun signalFileNotFound() {
        failableValueSlots.forEach {
            it.admitFailureIfNotDone("File not found: $cborFile")
        }
        (fileFound as BindableProperty).value = false
    }

    protected fun signalStreamNotOk() {
        failableValueSlots.forEach {
            it.admitFailureIfNotDone("Stream Not Ok")
        }
        (streamOk as BindableProperty).value = false
    }

    protected fun signalParseError(e: Exception) {
        failableValueSlots.forEach {
            it.admitFailureIfNotDone(e.toString())
        }
        parseError.value = e
    }

    protected fun signalFinishedLoading() {
        (finishedLoading as BindableProperty).value = true
    }

    @SupertypeIdea(SupertypeIdeaType.Failable)
    sealed interface LoadedOrFailed<T> {
        @Open
        fun requireLoaded() = (this as Loaded<T>).value
    }

    @JvmInline
    value class Loaded<T>(val value: T) : LoadedOrFailed<T>
    class Failed<T>(val message: String) : LoadedOrFailed<T> {
        override fun toString(): String = "Failed: $message"
    }

    private val failableValueSlots = mutableListOf<DirectLoadedOrFailedValueSlot<*>>()

    interface LoadedOrFailedValueSlot<T>: ThreadAwaitable<T> {
        fun isDone(): Boolean
    }
    inner class DirectLoadedOrFailedValueSlot<T> : Async<LoadedOrFailed<T>>(), LoadedOrFailedValueSlot<LoadedOrFailed<T>> {

        init {
            failableValueSlots += this
        }

        fun putLoadedValue(t: T) {
            monitor.withLock {
                require(latch!!.isClosed)
                value = Loaded(t)
                openAndDisposeLatch()
            }
        }

        fun admitFailureIfNotDone(message: String) {
            if (!isDone()) {
                require(latch!!.isClosed)
                value = Failed(message)
                openAndDisposeLatch()
            }
        }

        override fun isDone() = latch?.isOpen != false

        fun awaitRequireSuccessful() = (await() as Loaded).value
        fun awaitSuccessfulOrNull() = (await() as? Loaded)?.value
        fun awaitSuccessfulOrMessage() = await().let { (it as? Loaded)?.value ?: (it as Failed).message }

        fun getOrNullIfLoading() = if (isDone()) value!! else null

        fun <R> chainedTo(op: (T) -> DirectLoadedOrFailedValueSlot<R>): LoadedOrFailedValueSlot<LoadedOrFailed<R>> =
            ChainedLoadedValueSlot(
                this,
                op
            )
    }

    private inner class ChainedLoadedValueSlot<T, R>(
        private val first: DirectLoadedOrFailedValueSlot<T>,
        private val map: (T) -> DirectLoadedOrFailedValueSlot<R>
    ): LoadedOrFailedValueSlot<LoadedOrFailed<R>> {

        private var derived: DirectLoadedOrFailedValueSlot<R>? = null

        private val monitor = SimpleReferenceMonitor()

        override fun isDone(): Boolean {
            monitor.withLock {
                val der = derived
                if (der == null) {
                    if (first.isDone()) {
                        derived = map(first.getOrNullIfLoading()!!.requireLoaded())
                    }
                }
                return (derived?.isDone() == true)
            }
        }

        override fun await(): LoadedOrFailed<R> {
            val _ = first.await()
            val deriv =
                monitor.withLock {
                    val der = derived
                    if (der == null) {
                        derived = map(first.getOrNullIfLoading()!!.requireLoaded())
                    }
                    derived!!
                }
            return deriv.await()
        }
    }
}
