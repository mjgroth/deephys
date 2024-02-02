package matt.nn.deephys.load

import kotlinx.serialization.SerializationException
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import matt.file.JioFile
import matt.fx.graphics.wrapper.EventTargetWrapper
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperNeverNull
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.lang.model.file.FsFile
import matt.nn.deephys.load.async.AsyncLoader
import matt.obs.bind.binding
import matt.obs.prop.ObsVal
import kotlin.time.Duration

sealed interface CborSyncLoadResult<T>

class FileNotFound<T>(val f: FsFile): CborSyncLoadResult<T>
class ParseError<T>(val message: String?): CborSyncLoadResult<T>
class Loaded<T>(val data: T): CborSyncLoadResult<T>


inline fun <reified T: Any> JioFile.loadCbor(): CborSyncLoadResult<T> =
    if (doesNotExist) FileNotFound(this) else try {
        val bytes = readBytes()
        Loaded(Cbor.decodeFromByteArray(bytes))
    } catch (e: SerializationException) {
        ParseError(e.message)
    }

fun <T> EventTargetWrapper.loadSwapper(
    prop: ObsVal<CborSyncLoadResult<T>?>,
    nullMessage: String = "please select a file",
    op: T.()->NodeWrapper
) = swapper(prop, nullMessage) {
    when (this) {
        is FileNotFound -> TextWrapper("$f not found")
        is ParseError   -> TextWrapper("parse error: $message")
        is Loaded<T>    -> op(this.data)
    }
}

fun <T: AsyncLoader> EventTargetWrapper.asyncLoadSwapper(
    loader: ObsVal<T?>,
    nullMessage: String = "please select a file",
    fadeOutDur: Duration? = null,
    fadeInDur: Duration? = null,
    op: T.()->NodeWrapper
) = swapper(loader, nullMessage) {
    VBoxWrapperImpl<NodeWrapper>().also {

        it.swapperNeverNull(
            fileFound.binding(streamOk, parseError) { this },
            fadeOutDur = fadeOutDur,
            fadeInDur = fadeInDur
        ) {
            when {
                !fileFound.value         -> TextWrapper("file not found")
                !streamOk.value          -> TextWrapper("file loading stream broken. Was the file moved?")
                parseError.value != null -> TextWrapper("Encountered error while loading file: ${parseError.value?.message}")
                else                     -> op(this)
            }
        }
    }
}
