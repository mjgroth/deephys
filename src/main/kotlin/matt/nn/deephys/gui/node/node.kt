package matt.nn.deephys.gui.node

import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.nn.deephys.gui.settings.DeephysSettingsController

interface DeephysNode: NodeWrapper {
  val settings: DeephysSettingsController
}