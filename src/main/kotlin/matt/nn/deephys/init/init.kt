package matt.nn.deephys.init

import javafx.scene.image.Image
import matt.async.thread.TheThreadProvider
import matt.async.thread.daemon
import matt.file.thismachine.thisMachine
import matt.file.toJioFile
import matt.fx.control.toggle.mech.ToggleMechanism
import matt.fx.graphics.style.DarkModeController
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.HBoxWrapperImpl
import matt.lang.anno.optin.ExperimentalMattCode
import matt.log.profile.stopwatch.tic
import matt.model.flowlogic.latch.asyncloaded.DaemonLoadedValueOp
import matt.nn.deephys.gui.global.DeephyText
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyToggleButton
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.load.loadCbor
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
import matt.obs.bind.binding
import matt.rstruct.loader.desktop.systemResourceLoader

fun initializeWhatICan() {
    val t = tic("initializeWhatICan")
    t.toc("START")

    gearImage.startLoading()
    modelBinding.startLoading()

    daemon("initializeWhatICan inner Thread") {
        DarkModeController.darkModeProp.value
        t.toc("END DarkModeController DAEMON")
    }



    t.toc("END")
}


val gearImage =
    DaemonLoadedValueOp(TheThreadProvider, "gear.png") {
        Image(systemResourceLoader().resourceStream("gear.png"))
    }

@OptIn(ExperimentalMattCode::class)
val modelBinding =
    DaemonLoadedValueOp(TheThreadProvider, ".model binding") {
        DeephyState.model.binding { f ->
            f?.toJioFile()?.withinFileSystem(thisMachine.fileSystemFor(f.path))?.loadCbor<Model>()
        }
    }

fun warmupFxComponents(settings: DeephysSettingsController) {
    HBoxWrapperImpl<NodeWrapper>().apply {
        DeephyText("placeholder").apply {
            subtitleFont()
            titleFont()
            titleBoldFont()


            deephyTooltip("placeholder", settings = settings)

            /*
            Exception in thread "Thread-2" java.lang.ExceptionInInitializerError
              at matt.fx.control.wrapper.tooltip.tooltipWrapper.<init>(tooltip.kt:52)

              this ended up being due to YourKit..
             * */
        }
        deephyButton("placeholder")
        deephyToggleButton("placeholder", 0.0, ToggleMechanism())
    }
}
