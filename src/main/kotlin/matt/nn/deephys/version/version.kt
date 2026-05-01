package matt.nn.deephys.version

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import matt.compose.graphics.text.MyText
import matt.exec.app.deephysSite
import matt.exec.app.myVersion
import matt.http.json.requireIs
import matt.http.tryHttp
import matt.lang.controlflow.forever
import matt.model.context.SuspendingAutomationService
import matt.model.data.release.Version
import matt.model.data.release.VersionInfo
import matt.model.j.openUrl
import matt.model.k.log.Logger
import matt.model.k.log.warnPrefixed
import matt.model.k.osi.url.MURL
import matt.nn.deephys.gui.global.DeephyHyperlink
import matt.nn.deephys.gui.global.DeephysText
import matt.prim.exportfromlang.cfnf.getorthrow.getOrThrow
import java.net.ConnectException
import java.net.URI
import kotlin.time.Duration.Companion.seconds

object VersionChecker {

    private val error = mutableStateOf(false)
    private var checking = false

    context(_: Logger)
    fun start(scope: CoroutineScope) {
        scope.launch {
            forever {
                checking = true
                try {

                    val resp =
                        tryHttp(
                            MURL(deephysSite)/*.productionHost*/ + "latest-version"
                        ).getOrThrow() /*because FX IS DEAD*/
                    val latestVersionFromServer =
                        if (resp.statusCode() != HttpStatusCode.OK) {
                            null
                        } else {
                            resp.requireIs<VersionInfo>()
                        }
                    if (latestVersionFromServer == null) {
                        warnPrefixed("latestVersionFromServer == null")
                        error.value = true
                        return@launch
                    } else {
                        newestRelease.value = latestVersionFromServer
                    }
                } catch (_: ConnectException) {
                    println("no internet to check version")
                } finally {
                    checking = false
                }
                delay(60.seconds)
            }
        }
    }

    private val newestRelease = mutableStateOf<VersionInfo?>(null)

    @Composable
    context(automationContext: SuspendingAutomationService)
    fun statusNode() {

        val scope = rememberCoroutineScope()
        if (!error.value) {
            when (val new = newestRelease.value) {
                null if checking                  -> MyText("checking for updates...")

                is Any if new.version > myVersion -> {
                    DeephysText(s = "Version ${new.version} Available: ")
                    DeephyHyperlink("Click here to update") {
                        scope.launch {
                            automationContext.openUrl(URI(new.downloadURL))
                        }
                    }
                }

                is Any if new.version < myVersion -> {
                    DeephysText(s = "developing unreleased version (last pushed was $new)")
                }
            }
        }
    }
}

@Suppress("unused")
class VersionStatus(
    private val current: Version,
    private val latestRelease: Version
) {
    val updateAvailable by lazy { current != latestRelease }
}
