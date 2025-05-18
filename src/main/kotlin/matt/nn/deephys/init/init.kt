package matt.nn.deephys.init

import matt.async.thread.TheThreadProvider
import matt.async.thread.daemon
import matt.image.common.Png
import matt.lang.common.unsafeError
import matt.log.profile.stopwatch.tic
import matt.model.flowlogic.latch.asyncloaded.DaemonLoadedValueOp
import matt.prim.j.bs.readAllBytesAsByteString
import matt.rstruct.loader.desktop.systemResourceLoader

fun initializeWhatICan() {
    val t = tic("initializeWhatICan")
    t.toc("START")

    gearImage.startLoading()

    unsafeError(
        """
             /*modelBinding*/
    DaemonLoadedValueOp<Any>(TheThreadProvider, ".model binding") {
        deephyState.model.binding { f ->
            f?.toJioFile()?.withinFileSystem(thisMachine.fileSystemFor(f.path))?.loadCbor<Model>()
        }
    }.startLoading()
   
        """.trimIndent()
    )

    daemon("initializeWhatICan inner Thread") {
        unsafeError(
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



