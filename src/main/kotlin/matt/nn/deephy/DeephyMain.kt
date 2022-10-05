package matt.nn.deephy

import matt.async.thread.daemon
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.gui.app.warmup.warmupJvmThreading
import matt.log.profile.tic
import matt.nn.deephy.gui.stageTitle
import matt.nn.deephy.gui.startDeephyApp
import matt.nn.deephy.init.initializeWhatICan
import matt.nn.deephy.state.DeephySettingsNode
import matt.nn.deephy.state.DeephyState
import matt.nn.deephy.version.VersionChecker
import java.util.prefs.Preferences


fun main(args: Array<String>) {
  if (args.size == 1 && args[0] == "erase-state") {
	DeephyState.delete()
  } else if (args.size == 1 && args[0] == "erase-settings") {
	DeephySettingsNode.delete()
  } else {
	val t = tic("main method", enabled = false)
	t.toc("starting main method")
	warmupJvmThreading()
	t.toc("warmed up jvm threading")

	daemon {
	  stageTitle.putLoadedValue("${appName}s $myVersion")
	}
	t.toc("started async stage title getter")

	daemon {
	  initializeWhatICan()
	}
	t.toc("started initializing what I can")


	VersionChecker.checkForUpdatesInBackground()
	t.toc("started VersionChecker")

	daemon {
	  Preferences.userRoot().node("sinhalab.deephy.state").apply {
		removeNode()
		flush()
	  }
	  Preferences.userRoot().node("sinhalab.deephy.settings").apply {
		removeNode()
		flush()
	  }
	}

	startDeephyApp(t)
	t.toc("started Deephy app")
  }
}