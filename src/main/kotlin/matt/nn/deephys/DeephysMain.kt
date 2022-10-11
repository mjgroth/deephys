package matt.nn.deephys

import matt.async.thread.daemon
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.gui.app.warmup.warmupJvmThreading
import matt.nn.deephys.gui.stageTitle
import matt.nn.deephys.gui.startDeephyApp
import matt.nn.deephys.init.initializeWhatICan
import matt.nn.deephys.state.DeephySettingsNode
import matt.nn.deephys.state.DeephyState
import matt.nn.deephys.version.VersionChecker
import java.util.prefs.Preferences


fun main(args: Array<String>) {
  if (args.size == 1 && args[0] == "erase-state") {
	DeephyState.delete()
  } else if (args.size == 1 && args[0] == "erase-settings") {
	DeephySettingsNode.delete()
  } else {
	warmupJvmThreading()

	daemon {
	  stageTitle.putLoadedValue("${appName}s $myVersion")
	}

	daemon {
	  initializeWhatICan()
	}


	VersionChecker.checkForUpdatesInBackground()

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

	startDeephyApp()
  }
}