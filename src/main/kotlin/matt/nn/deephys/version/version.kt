package matt.nn.deephys.version

import javafx.application.Platform.runLater
import kotlinx.coroutines.runBlocking
import matt.async.pool.MyThreadPriorities
import matt.async.schedule.AccurateTimer
import matt.async.schedule.every
import matt.async.thread.daemon
import matt.exec.app.myVersion
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.text.text
import matt.fx.graphics.wrapper.textflow.TextFlowWrapper
import matt.kjlib.git.hub.GH_ORG_NAME
import matt.kjlib.git.hub.GitHubClient
import matt.kjlib.git.hub.mainPageOf
import matt.log.warn.warnOnce
import matt.model.data.release.Release
import matt.model.data.release.Version
import matt.mstruct.rstruct.appName
import matt.mstruct.rstruct.modID
import matt.nn.deephys.gui.global.deephyHyperlink
import matt.nn.deephys.gui.global.deephysText
import matt.obs.prop.BindableProperty
import matt.time.dur.sec
import java.net.ConnectException

object VersionChecker {
  private val gh by lazy { GitHubClient() }
  private var error = false
  private var checking = false
  fun checkForUpdatesInBackground() = daemon {
	every(60.sec, timer = AccurateTimer(priority = MyThreadPriorities.CREATING_NEW_CACHE), zeroDelayFirst = true) {
	  checking = true
	  try {
		val releases = runBlocking {gh.GitHubRepo(gh.Organization(GH_ORG_NAME), modID.appName).unAuthenticatedReleases()}
		if (releases == null) {
		  warnOnce("releases == null")
		  error = true
		  runLater { update(null) }
		  cancel()
		} else {
		  val newest = releases.maxBy { it.version }
		  runLater {
			newestRelease.setIfDifferent(newest)
		  }
		}
	  } catch (e: ConnectException) {
		println("no internet to check version")
	  } finally {
		checking = false
	  }
	}
  }

  private val newestRelease by lazy { BindableProperty<Release?>(null) }

  val statusNode by lazy {
	TextFlowWrapper<NodeWrapper>().apply {

	}
  }

  private fun update(new: Release?) = statusNode.apply {
	clear()
	if (!error) {
	  if (new == null && checking) text("checking for updates...")
	  else if (new != null && new.version > myVersion) {
		deephysText("Version ${new.version} Available: ")
		deephyHyperlink("Click here to update") {
		  opens(gh.Organization(GH_ORG_NAME).mainPageOf(appName).jURL.toURI())
		}
	  } else if (new != null && new.version < myVersion) {
		deephysText("developing unreleased version (last pushed was ${new.version})")
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