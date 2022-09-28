package matt.nn.deephy.gui.neuron.imgflowpane

import matt.hurricanefx.wrapper.pane.flow.FlowPaneWrapper
import matt.nn.deephy.gui.deephyimview.DeephyImView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.obs.bindings.math.times

open class ImageFlowPane(viewer: DatasetViewer): FlowPaneWrapper<DeephyImView>() {
  init {

	/*for reasons I don't understand, without this this FlowPane gets really over-sized in the y dimension*/
	prefWrapLengthProperty.bind(viewer.widthProperty*0.8)

  }
}
