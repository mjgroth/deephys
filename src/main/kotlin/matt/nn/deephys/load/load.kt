@file:Suppress("NoDuplicatedTypeNames")

package matt.nn.deephys.load

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import matt.cbor.my.MyCbor
import matt.compose.graphics.Compose
import matt.compose.graphics.text.MyText
import matt.file.JioFile
import matt.lang.generic.GenericFailable
import matt.lang.generic.on
import matt.lang.safeconvert.verifyToInt
import matt.model.obj.text.doesNotExist
import matt.nn.deephys.load.async.AsyncLoader
import matt.obs.prop.ObsVal
import matt.prim.common.exportfromlang.model.file.FsFile
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.time.Duration

typealias CborSyncLoadResult<T> = GenericFailable<Loaded<T>, CborSyncLoadFailure>

sealed interface CborSyncLoadFailure
class FileNotFound(val f: FsFile): CborSyncLoadFailure
class ParseError(val message: String?): CborSyncLoadFailure
class Loaded<T>(val data: T)

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T: Any> JioFile.loadCbor(): CborSyncLoadResult<T> =
    if (doesNotExist()) GenericFailable.failure(FileNotFound(this)) else try {
        GenericFailable.success(Loaded(MyCbor.decodeFromByteArray((this as Path).readBytes())))
    } catch (e: SerializationException) {
        GenericFailable.failure(ParseError(e.message))
    }

@Suppress("unused")
@Composable
fun <T> LoadSwapper(
    prop: ObsVal<CborSyncLoadResult<T>?>,
    nullMessage: String = "please select a file",
    op: @Composable T.() -> Unit
) {
    LoadSwapper(
        value = prop.value,
        nullMessage = nullMessage,
        op = op
    )
}

@Composable
fun <T> LoadSwapper(
    value: CborSyncLoadResult<T>?,
    nullMessage: String = "please select a file",
    op: @Composable T.() -> Unit
) {
    value?.let { result ->
        result.on(
            success = {
                op(it.data)
            },
            failure = {
                when (it) {
                    is FileNotFound -> MyText("${it.f} not found")
                    is ParseError   -> MyText("parse error: ${it.message}")
                }
            }
        )
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
        val theContent =
            when {
                v == null                  -> {
                    MyText(nullMessage)
                    null
                }

                !v.fileFound.value         -> {
                    MyText("file not found")
                    null
                }

                !v.streamOk.value          -> {
                    MyText("file loading stream broken. Was the file moved?")
                    null
                }

                v.parseError.value != null -> {
                    MyText("Encountered error while loading file: ${v.parseError.value?.message}")
                    null
                }

                else                       -> content(v)
            }

        AnimatedVisibility(
            visible = theContent != null,
            enter = fadeInDur?.let { fadeIn(animationSpec = tween(durationMillis = it.inWholeMilliseconds.verifyToInt())) } ?: EnterTransition.None,
            exit = fadeOutDur?.let {  fadeOut(animationSpec = tween(durationMillis = it.inWholeMilliseconds.verifyToInt())) } ?: ExitTransition.None
        ) {
            theContent?.invoke()
        }
    }
}
