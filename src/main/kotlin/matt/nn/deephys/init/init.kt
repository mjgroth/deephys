package matt.nn.deephys.init

import matt.async.thread.TheThreadProvider
import matt.async.thread.daemon
import matt.file.thismachine.thisMachine
import matt.file.toJioFile
import matt.image.common.Png
import matt.lang.anno.optin.ExperimentalMattCode
import matt.lang.common.unsafeErr
import matt.log.profile.stopwatch.tic
import matt.model.flowlogic.latch.asyncloaded.DaemonLoadedValueOp
import matt.nn.deephys.load.loadCbor
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
import matt.obs.bind.binding
import matt.prim.j.bs.readAllBytesAsByteString
import matt.rstruct.loader.desktop.systemResourceLoader

fun initializeWhatICan() {
    val t = tic("initializeWhatICan")
    t.toc("START")

    gearImage.startLoading()
    modelBinding.startLoading()

    daemon("initializeWhatICan inner Thread") {
        unsafeErr(
            """
            DarkModeController.darkModeProp.value    
            """.trimIndent()
        )
        t.toc("END DarkModeController DAEMON")
    }



    t.toc("END")
}


val gearImage =
    DaemonLoadedValueOp(TheThreadProvider, "gear.png") {
        Png(systemResourceLoader().resourceStream("gear.png")!!.readAllBytesAsByteString())
    }

@OptIn(ExperimentalMattCode::class)
val modelBinding =
    DaemonLoadedValueOp(TheThreadProvider, ".model binding") {
        DeephyState.model.binding { f ->
            f?.toJioFile()?.withinFileSystem(thisMachine.fileSystemFor(f.path))?.loadCbor<Model>()
        }
    }

