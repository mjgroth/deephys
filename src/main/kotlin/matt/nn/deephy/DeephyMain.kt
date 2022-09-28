package matt.nn.deephy

import matt.async.thread.daemon
import matt.auto.myPid
import matt.log.profile.MemReport
import matt.nn.deephy.gui.startDeephyApp
import matt.nn.deephy.version.VersionChecker
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.minutes


fun main() {

  VersionChecker.checkForUpdatesInBackground()
  println("my pid = $myPid")
  daemon {
	while (true) {
	  println(MemReport())
	  sleep(1.minutes.inWholeMilliseconds)
	}
  }

  startDeephyApp()

}