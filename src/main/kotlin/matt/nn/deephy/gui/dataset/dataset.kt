package matt.nn.deephy.gui.dataset

import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.anchor.swapper.Swapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.region.RegionWrapper
import matt.nn.deephy.gui.DEEPHY_FONT
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephy.gui.dataset.byimage.ByImageView
import matt.nn.deephy.gui.dataset.byneuron.ByNeuronView
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.Test

enum class DatasetNodeView {
  ByNeuron, ByImage
}

class DatasetNode(
  dataset: Test, viewer: DatasetViewer
): Swapper<DatasetNodeView, RegionWrapper<*>>() {


  private val byNeuronView by lazy { ByNeuronView(dataset, viewer) }
  private val byImageView by lazy { ByImageView(dataset, viewer) }

  init {
	setupSwapping(viewer.view) {
	  VBoxWrapper<NodeWrapper>().apply {
		val layerCB = choicebox(property = viewer.layerSelection, values = viewer.model.resolvedLayers) {
		  valueProperty.onChange {
			println("layerCB value changed to $it")
		  }
		}
		hbox<NodeWrapper> {
		  text("layer: ") {
			font = DEEPHY_FONT
		  }
		  +layerCB
		  visibleAndManagedProp.bind(viewer.isUnboundToDSet)
		}
		add(
		  when (this@setupSwapping) {
			ByNeuron -> this@DatasetNode.byNeuronView
			ByImage  -> this@DatasetNode.byImageView
		  }
		)
	  }

	}
  }
}