package matt.nn.deephys.init

import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import kotlinx.serialization.ExperimentalSerializationApi
import matt.async.thread.daemon
import matt.file.toMFile
import matt.fx.graphics.style.DarkModeController
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.HBoxWrapperImpl
import matt.lang.resourceStream
import matt.log.profile.stopwatch.tic
import matt.model.latch.asyncloaded.DaemonLoadedValueOp
import matt.nn.deephys.gui.global.DeephyText
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyToggleButton
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.load.loadCbor
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
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


@OptIn(ExperimentalSerializationApi::class)
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