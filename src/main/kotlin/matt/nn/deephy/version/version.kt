package matt.nn.deephy.version

import javafx.application.Platform.runLater
import javafx.beans.property.SimpleObjectProperty
import matt.async.schedule.AccurateTimer
import matt.async.schedule.every
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.setIfDifferent
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.text.text
import matt.fx.graphics.wrapper.textflow.TextFlowWrapper
import matt.kjlib.git.hub.GitHub
import matt.log.warn
import matt.model.release.Release
import matt.model.release.Version
import matt.nn.deephy.gui.global.deephyHyperlink
import matt.nn.deephy.gui.global.deephyText
import matt.time.dur.sec
import java.net.ConnectException

object VersionChecker {
  private var checking = false
  fun checkForUpdatesInBackground() {
	if (!checking) {
	  every(60.sec, timer = AccurateTimer(), zeroDelayFirst = true) {
		try {
		  val releases = GitHub.releasesOf(appName)
		  if (releases == null) {
			warn("releases == null")
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

  private val newestRelease = SimpleObjectProperty<Release>()

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