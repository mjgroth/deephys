package matt.nn.deephys.gui.dataset.bycategory

import matt.fx.control.wrapper.control.choice.choicebox
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.nn.deephys.gui.category.CategoryView
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.testlike.TypedTestLike

class ByCategoryView(
  testLoader: TypedTestLike<*>,
  viewer: DatasetViewer
): VBoxWrapperImpl<RegionWrapper<*>>() {
  init {
	val categoryCB = choicebox(
	  nullableProp = viewer.categorySelection,
	  values = testLoader.test.categories
	) {
	  //	  converter = toStringConverter<InterTestNeuron?> { "neuron ${it?.index}" }.toFXConverter()
	}
	hbox<NodeWrapper> {
	  deephyText("category: ")
	  +categoryCB
	  //	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull)
	}
	swapper(
	  categoryCB.valueProperty,
	  nullMessage = "select a category",
	  fadeOutDur = DEEPHYS_FADE_DUR,
	  fadeInDur = DEEPHYS_FADE_DUR
	) {
	  CategoryView(this, testLoader = testLoader, viewer = viewer)
	}
  }
}