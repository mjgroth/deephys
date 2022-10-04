package matt.nn.deephy.init

import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import matt.async.thread.daemon
import matt.file.toMFile
import matt.fx.graphics.style.DarkModeController
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.HBoxWrapperImpl
import matt.lang.resourceStream
import matt.log.profile.tic
import matt.model.latch.asyncloaded.DaemonLoadedValueOp
import matt.nn.deephy.gui.global.DeephyText
import matt.nn.deephy.gui.global.deephyButton
import matt.nn.deephy.gui.global.deephyToggleButton
import matt.nn.deephy.gui.global.deephyTooltip
import matt.nn.deephy.gui.global.subtitleFont
import matt.nn.deephy.gui.global.titleBoldFont
import matt.nn.deephy.gui.global.titleFont
import matt.nn.deephy.load.loadCbor
import matt.nn.deephy.model.importformat.Model
import matt.nn.deephy.state.DeephyState
import matt.obs.bind.binding

fun initializeWhatICan() {
  val t = tic("initializeWhatICan")
  t.toc("START")

  gearImage.startLoading()
  modelBinding.startLoading()

  daemon {
	DarkModeController.darkModeProp.value
	t.toc("END DarkModeController DAEMON")
  }

  warmupFxComponents()

  t.toc("END")
}


val gearImage = DaemonLoadedValueOp("gear.png") {
  Image(resourceStream("gear.png"))
}


val modelBinding = DaemonLoadedValueOp(".model binding") {
  DeephyState.model.binding { f ->
	f?.toMFile()?.loadCbor<Model>()
  }
}

private fun warmupFxComponents() {
  HBoxWrapperImpl<NodeWrapper>().apply {
	DeephyText("placeholder").apply {
	  subtitleFont()
	  titleFont()
	  titleBoldFont()
	  deephyTooltip("placeholder")
	}
	deephyButton("placeholder")
	deephyToggleButton("placeholder", 0.0, ToggleGroup())
  }
}