package matt.nn.deephys.gui.dataset

import matt.fx.control.wrapper.control.choice.choicebox
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.Swapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dataset.bycategory.ByCategoryView
import matt.nn.deephys.gui.dataset.byimage.ByImageView
import matt.nn.deephys.gui.dataset.byneuron.ByNeuronView
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader

enum class DatasetNodeView {
  ByNeuron, ByImage, ByCategory
}

class DatasetNode(
  dataset: TestLoader, viewer: DatasetViewer
): Swapper<DatasetNodeView, RegionWrapper<*>>() {


  private val byNeuronView by lazy { ByNeuronView(dataset.preppedTest.await(), viewer) }
  private val byImageView by lazy { ByImageView(dataset.preppedTest.await(), viewer) }
  private val byCategoryView by lazy { ByCategoryView(dataset.preppedTest.await(), viewer) }

  init {
	setupSwapping(
	  viewer.view,
	  fadeOutDur = DEEPHYS_FADE_DUR,
	  fadeInDur = DEEPHYS_FADE_DUR
	) {
	  VBoxWrapperImpl<NodeWrapper>().apply {
		val layerCB =
		  choicebox(nullableProp = viewer.layerSelection, values = viewer.model.resolvedLayers.map { it.interTest }) {
			valueProperty.onChange {
			  println("layerCB value changed to $it")
			}
		  }
		hbox<NodeWrapper> {
		  deephyText("layer: ")
		  +layerCB
		  visibleAndManagedProp.bind(viewer.isUnboundToDSet)

		}
		add(
		  when (this@setupSwapping) {
			ByNeuron   -> this@DatasetNode.byNeuronView
			ByImage    -> this@DatasetNode.byImageView
			ByCategory -> this@DatasetNode.byCategoryView
		  }
		)
	  }

	}
  }
}