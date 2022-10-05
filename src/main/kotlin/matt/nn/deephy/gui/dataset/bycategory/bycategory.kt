package matt.nn.deephy.gui.dataset.bycategory

import matt.fx.control.wrapper.control.choice.choicebox
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.nn.deephy.gui.category.CategoryView
import matt.nn.deephy.gui.global.deephyText
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.load.test.TestLoader

class ByCategoryView(
  testLoader: TestLoader,
  viewer: DatasetViewer
): VBoxWrapperImpl<RegionWrapper<*>>() {
  init {
	val categoryCB = choicebox(
	  property = viewer.categorySelection,
	  values = testLoader.awaitFinishedTest().categories
	) {
	  //	  converter = toStringConverter<InterTestNeuron?> { "neuron ${it?.index}" }.toFXConverter()
	}
	hbox<NodeWrapper> {
	  deephyText("category: ")
	  +categoryCB
	  //	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull)
	}
	swapper(categoryCB.valueProperty, nullMessage = "select a category") {
	  CategoryView(this, testLoader = testLoader, viewer = viewer)
	}
  }
}