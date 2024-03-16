package matt.nn.deephys.gui.neuron.imgflowpane

import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.flow.FlowPaneWrapper
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.obs.math.double.op.times

class ImageFlowPane(viewer: DatasetViewer): FlowPaneWrapper<NodeWrapper>(childClass = NodeWrapper::class) {
    init {

        /*for reasons I don't understand, without this this FlowPane gets really over-sized in the y dimension*/
        prefWrapLengthProperty.bind(viewer.widthProperty * 0.8)
    }
}
