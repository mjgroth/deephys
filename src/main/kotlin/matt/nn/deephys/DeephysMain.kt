package matt.nn.deephys

import matt.nn.deephys.gui.DeephysApp

/*NOT INVOKED BY TEST in case I ever want the main test method to return something*/
fun main(args: Array<String>) {
  System.err.println("this is from std err")
  DeephysApp().boot(args)
}