package matt.nn.deephy.load

import kotlinx.serialization.SerializationException
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import matt.file.MFile
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.target.EventTargetWrapper
import matt.hurricanefx.wrapper.text.TextWrapper
import matt.obs.prop.ObsVal

class FileNotFound<T>(val f: MFile): CborTestLoadResult<T>
class ParseError<T>(val message: String?): CborTestLoadResult<T>
class Loaded<T>(val data: T): CborTestLoadResult<T>

sealed interface CborTestLoadResult<T>

inline fun <reified T: Any> MFile.loadCbor(): CborTestLoadResult<T> = if (doesNotExist) FileNotFound(this) else try {
  Loaded(Cbor.decodeFromByteArray(readBytes()))
} catch (e: SerializationException) {
  ParseError(e.message)
}

fun <T> EventTargetWrapper.loadSwapper(
  prop: ObsVal<CborTestLoadResult<T>?>,
  nullMessage: String = "please select a file",
  op: T.()->NodeWrapper
) = swapper(prop, nullMessage) {
  when (this) {
	is FileNotFound -> TextWrapper("$f not found")
	is ParseError   -> TextWrapper("parse error: $message")
	is Loaded<T>    -> op(this.data)
  }
}