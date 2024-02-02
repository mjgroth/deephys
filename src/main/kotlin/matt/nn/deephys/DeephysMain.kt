package matt.nn.deephys

import matt.lang.shutdown.ShutdownExecutorImpl
import matt.model.code.args.Arguments
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.DeephysArgs

fun main(args: Array<String>): Unit = main(Arguments.decodeFromArgs<DeephysArgs>(args))

/*NOT INVOKED BY TEST in case I ever want the main test method to return something*/
fun main(args: DeephysArgs) {
    with(ShutdownExecutorImpl()) {
        //  ES2Graphics
//    BugReport(Thread.currentThread(), Exception()).print()
        DeephysApp().boot(args)
    }

}

//@Serializable
//class DeephysArgs()
