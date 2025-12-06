package matt.nn.deephys.init

import matt.async.thread.TheThreadProvider
import matt.image.common.Png
import matt.model.flowlogic.latch.asyncloaded.DaemonLoadedValueOp
import matt.prim.j.bs.readAllBytesAsByteString
import matt.rstruct.loader.desktop.systemResourceLoader

val gearImage =
    DaemonLoadedValueOp(TheThreadProvider, "gear.png") {
        Png(systemResourceLoader().resourceStream("gear.png")!!.readAllBytesAsByteString())
    }
