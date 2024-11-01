package matt.nn.deephys.load

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import matt.cbor.my.MyCbor
import matt.compose.graphics.Compose
import matt.compose.graphics.text.MyText
import matt.file.JioFile
import matt.lang.model.file.FsFile
import matt.nn.deephys.load.async.AsyncLoader
import matt.obs.prop.ObsVal
import kotlin.io.path.readBytes
import kotlin.time.Duration

sealed interface CborSyncLoadResult<T>

class FileNotFound<T>(val f: FsFile): CborSyncLoadResult<T>
class ParseError<T>(val message: String?): CborSyncLoadResult<T>
class Loaded<T>(val data: T): CborSyncLoadResult<T>


@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T: Any> JioFile.loadCbor(): CborSyncLoadResult<T> =
    if (doesNotExist) FileNotFound(this) else try {
        val bytes = readBytes()
        Loaded(MyCbor.decodeFromByteArray(bytes))
    } catch (e: SerializationException) {
        ParseError(e.message)
    }

@Composable
fun <T> LoadSwapper(
    prop: ObsVal<CborSyncLoadResult<T>?>,
    nullMessage: String = "please select a file",
    op: @Composable T.() -> Unit
) {
    prop.value?.let {
        when (it) {
            is FileNotFound -> MyText("${it.f} not found")
            is ParseError   -> MyText("parse error: ${it.message}")
            is Loaded<T>    -> op(it.data)
        }
    } ?: MyText(nullMessage)
}

@Composable
fun <T: AsyncLoader> AsyncLoadSwapper(
    loader: ObsVal<T?>,
    nullMessage: String = "please select a file",
    fadeOutDur: Duration? = null,
    fadeInDur: Duration? = null,
    content: (T) -> Compose
) {
    val v = loader.value
    Column {
        when {
            v == null                  -> MyText(nullMessage)
            !v.fileFound.value         -> MyText("file not found")
            !v.streamOk.value          -> MyText("file loading stream broken. Was the file moved?")
            v.parseError.value != null -> MyText("Encountered error while loading file: ${v.parseError.value?.message}")
            else                       -> content(v)
        }
    }
}
