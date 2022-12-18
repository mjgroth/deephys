package matt.nn.deephys.version

import javafx.application.Platform.runLater
import matt.async.schedule.AccurateTimer
import matt.async.schedule.every
import matt.async.thread.daemon
import matt.exec.app.myVersion
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.text.text
import matt.fx.graphics.wrapper.textflow.TextFlowWrapper
import matt.kjlib.git.hub.GitHub
import matt.kjlib.git.hub.GitHubRepo
import matt.log.warn.warn
import matt.log.warn.warnOnce
import matt.model.data.release.Release
import matt.model.data.release.Version
import matt.mstruct.rstruct.appName
import matt.nn.deephys.gui.global.deephyHyperlink
import matt.nn.deephys.gui.global.deephyText
import matt.obs.prop.BindableProperty
import matt.time.dur.sec
import java.net.ConnectException

object VersionChecker {
  private var checking = false
  fun checkForUpdatesInBackground() = daemon {
	if (!checking) {
	  every(60.sec, timer = AccurateTimer(), zeroDelayFirst = true) {
		try {
		  val releases = GitHubRepo(appName).releases()
		  if (releases == null) {
			warnOnce("releases == null")
		  } else {
			val newest = releases.maxBy { it.version }
			runLater {
			  newestRelease.setIfDifferent(newest)
			}
		  }
		} catch (e: ConnectException) {
		  println("no internet to check version")
		}
	  }
	}
	checking = true
  }

  private val newestRelease by lazy { BindableProperty<Release?>(null) }

  val statusNode by lazy {
	TextFlowWrapper<NodeWrapper>().apply {
	  fun update(new: Release?) {
		clear()
		if (new == null) text("checking for updates...")
		else if (new.version > myVersion) {
		  deephyText("Version ${new.version} Available: ")
		  deephyHyperlink("Click here to update") {
			opens(GitHub.mainPageOf(appName).jURL.toURI())
		  }
		} else if (new.version < myVersion) {
		  deephyText("developing unreleased version (last pushed was ${new.version})")
		}
	  }
	  update(newestRelease.value)
	  newestRelease.onChange { update(it) }
	}
  }
}

class VersionStatus(
  val current: Version,
  val latestRelease: Version
) {
  val updateAvailable by lazy { current != latestRelease }
}