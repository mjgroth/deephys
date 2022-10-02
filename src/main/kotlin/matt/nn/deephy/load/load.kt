package matt.nn.deephy.load

import kotlinx.serialization.SerializationException
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import matt.file.MFile
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.target.EventTargetWrapper
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.nn.deephy.load.async.AsyncLoader
import matt.obs.bind.binding
import matt.obs.prop.ObsVal

sealed interface CborSyncLoadResult<T>

class FileNotFound<T>(val f: MFile): CborSyncLoadResult<T>
class ParseError<T>(val message: String?): CborSyncLoadResult<T>
class Loaded<T>(val data: T): CborSyncLoadResult<T>

inline fun <reified T: Any> MFile.loadCbor(): CborSyncLoadResult<T> =
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
  op: T.()->NodeWrapper
) = swapper(loader, nullMessage) {
  VBoxWrapper<NodeWrapper>().also {

	it.swapper(fileFound.binding(streamOk) { this }) {
	  when {
		!fileFound.value -> TextWrapper("file not found")
		!streamOk.value  -> TextWrapper("file loading stream broken. Was the file moved?")
		parseError.value -> TextWrapper("parse error loading file")
		else             -> op(this)
	  }
	}
  }
}