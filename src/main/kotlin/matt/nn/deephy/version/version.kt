package matt.nn.deephy.version

import javafx.application.Platform.runLater
import javafx.beans.property.SimpleObjectProperty
import matt.async.date.sec
import matt.async.schedule.AccurateTimer
import matt.async.schedule.every
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.file.GitHub
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.setIfDifferent
import matt.hurricanefx.wrapper.textflow.TextFlowWrapper
import matt.klib.log.warn
import matt.klib.release.Release
import matt.klib.release.Version

object VersionChecker {
  private var checking = false
  fun checkForUpdatesInBackground() {
	if (!checking) {
	  every(60.sec, timer = AccurateTimer(), zeroDelayFirst = true) {
		val releases = GitHub.releasesOf(appName)
		if (releases == null) {
			warn("releases == null")
		} else {
		  val newest = releases.maxBy { it.version }
		  runLater {
			newestRelease.setIfDifferent(newest)
		  }
		}
	  }
	}
	checking = true
  }

  private val newestRelease = SimpleObjectProperty<Release>()

  val statusNode by lazy {
	TextFlowWrapper().apply {
	  fun update(new: Release?) {
		clear()
		if (new == null) text("checking for updates...")
		else if (new.version != myVersion) {
		  text("Version ${new.version} Available: ")
		  hyperlink("Click here to update") {
			opens(GitHub.releasesPageOf(appName).jURL.toURI())
		  }
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