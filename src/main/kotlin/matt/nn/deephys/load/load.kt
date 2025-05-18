@file:Suppress("unused", "NoDuplicatedTypeNames")

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
import matt.model.obj.text.doesNotExist
import matt.nn.deephys.load.async.AsyncLoader
import matt.obs.prop.ObsVal
import matt.prim.common.exportfromlang.model.file.FsFile
import java.nio.file.Path
import kotlin.io.path.readBytes

sealed interface CborSyncLoadResult<T>

class FileNotFound<T>(val f: FsFile): CborSyncLoadResult<T>
class ParseError<T>(val message: String?): CborSyncLoadResult<T>
class Loaded<T>(val data: T): CborSyncLoadResult<T>


@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T: Any> JioFile.loadCbor(): CborSyncLoadResult<T> =
    if (doesNotExist()) FileNotFound(this) else try {
        Loaded(MyCbor.decodeFromByteArray((this as Path).readBytes()))
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
