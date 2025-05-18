@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED", "unused")

package matt.nn.deephys.version

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import matt.async.pri.MyThreadPriority.CREATING_NEW_CACHE
import matt.async.thread.daemon
import matt.async.thread.schedule.AccurateTimer
import matt.async.thread.schedule.oldThreadedEvery
import matt.compose.graphics.text.MyText
import matt.exec.app.deephysSite
import matt.exec.app.myVersion
import matt.http.json.requireIs
import matt.http.tryHttp
import matt.lang.cfnf.getOrThrow
import matt.log.warn.common.warn
import matt.model.data.release.Version
import matt.model.data.release.VersionInfo
import matt.nn.deephys.gui.global.DeephyHyperlink
import matt.nn.deephys.gui.global.DeephysText
import matt.prim.common.exportfromlang.context.AutomationContext
import matt.prim.common.exportfromlang.model.url.MURL
import matt.prim.exportfromlang.j.openUrl
import matt.time.dur.common.sec
import java.net.ConnectException
import java.net.URI

object VersionChecker {

    private val error = mutableStateOf(false)
    private var checking = false
    fun checkForUpdatesInBackground() =
        daemon("VersionChecker Thread") {
            oldThreadedEvery(
                60.sec,
                timer =
                    AccurateTimer(
                        name = "VersionChecker Timer",
                        priority = CREATING_NEW_CACHE
                    ),
                zeroDelayFirst = true
            ) {
                checking = true
                try {
                    val latestVersionFromServer =
                        runBlocking {
                            val resp =
                                tryHttp(
                                    MURL(deephysSite)/*.productionHost*/ + "latest-version"
                                ).getOrThrow() /*because FX IS DEAD*/
                            if (resp.statusCode() != HttpStatusCode.OK) {
                                null
                            } else {
                                resp.requireIs<VersionInfo>()
                            }
                        }
                    if (latestVersionFromServer == null) {
                        warn("latestVersionFromServer == null")
                        error.value = true
                        cancel()
                    } else {
                        newestRelease.value = latestVersionFromServer
                    }
                } catch (e: ConnectException) {
                    println("no internet to check version")
                } finally {
                    checking = false
                }
            }
        }

    private val newestRelease = mutableStateOf<VersionInfo?>(null)

    context(AutomationContext)
    @Composable
    fun statusNode() {

        if (!error.value) {
            val new = newestRelease.value
            when (new) {
                null if checking                  -> MyText("checking for updates...")

                is Any if new.version > myVersion -> {
                    DeephysText("Version ${new.version} Available: ")
                    DeephyHyperlink("Click here to update") {
                        openUrl(URI(new.downloadURL))
                    }
                }

                is Any if new.version < myVersion -> {
                    DeephysText("developing unreleased version (last pushed was $new)")
                }
            }
        }
    }
}

class VersionStatus(
    private val current: Version,
    private val latestRelease: Version
) {
    val updateAvailable by lazy { current != latestRelease }
}
