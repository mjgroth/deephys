package matt.nn.deephys

import matt.lang.shutdown.j.ShutdownExecutorImpl
import matt.model.code.args.Arguments
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.DeephysArgs

fun main(args: Array<String>): Unit = Arguments.mainOrExitWithLogicalFailure<DeephysArgs>(args, ::main)

/*NOT INVOKED BY TEST in case I ever want the main test method to return something*/
fun main(args: DeephysArgs) {
    with(ShutdownExecutorImpl()) {
        DeephysApp().boot(args)
    }
}

