package matt.nn.deephys.version

import io.ktor.http.HttpStatusCode
import javafx.application.Platform.runLater
import kotlinx.coroutines.runBlocking
import matt.async.pri.MyThreadPriorities.CREATING_NEW_CACHE
import matt.async.thread.daemon
import matt.async.thread.schedule.AccurateTimer
import matt.async.thread.schedule.every
import matt.exec.app.myVersion
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.text.text
import matt.fx.graphics.wrapper.textflow.TextFlowWrapper
import matt.gui.exception.deephysSite
import matt.http.http
import matt.http.json.requireIs
import matt.http.url.MURL
import matt.log.warn.warn
import matt.model.data.release.Version
import matt.model.data.release.VersionInfo
import matt.nn.deephys.gui.global.deephyHyperlink
import matt.nn.deephys.gui.global.deephysText
import matt.obs.prop.BindableProperty
import matt.time.dur.sec
import java.net.ConnectException
import java.net.URI

object VersionChecker {

    private var error = false
    private var checking = false
    fun checkForUpdatesInBackground() = daemon("VersionChecker Thread") {
        every(
            60.sec,
            timer = AccurateTimer(
                name = "VersionChecker Timer",
                priority = CREATING_NEW_CACHE
            ),
            zeroDelayFirst = true
        ) {
            checking = true
            try {
                val latestVersionFromServer =
                    runBlocking {
                        val resp = http(MURL(deephysSite)/*.productionHost*/ + "latest-version")
                        if (resp.statusCode() != HttpStatusCode.OK) {
                            null
                        } else {
                            resp.requireIs<VersionInfo>()
                        }
                    }
                if (latestVersionFromServer == null) {
                    warn("latestVersionFromServer == null")
                    error = true
                    runLater { update(null) }
                    cancel()
                } else {
                    runLater {
                        newestRelease.setIfDifferent(latestVersionFromServer)
                    }
                }
            } catch (e: ConnectException) {
                println("no internet to check version")
            } finally {
                checking = false
            }
        }
    }

    private val newestRelease by lazy { BindableProperty<VersionInfo?>(null) }

    val statusNode by lazy {
        TextFlowWrapper<NodeWrapper>().apply {

        }
    }

    private fun update(new: VersionInfo?) = statusNode.apply {
        clear()
        if (!error) {
            if (new == null && checking) text("checking for updates...")
            else if (new != null && new.version > myVersion) {
                deephysText("Version ${new.version} Available: ")
                deephyHyperlink("Click here to update") {
                    opens(URI(new.downloadURL))
                }
            } else if (new != null && new.version < myVersion) {
                deephysText("developing unreleased version (last pushed was $new)")
            }
        }
    }

    init {
        update(newestRelease.value)
        newestRelease.onChange { update(it) }
    }

}

class VersionStatus(
    val current: Version,
    val latestRelease: Version
) {
    val updateAvailable by lazy { current != latestRelease }
}
