package matt.nn.deephys.gui.dataset.bycategory

import matt.fx.base.converter.toFXConverter
import matt.fx.control.wrapper.control.choice.choicebox
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.lang.go
import matt.model.op.convert.toStringConverter
import matt.nn.deephys.gui.category.CategoryView
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.deephysLabeledControl
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.obs.prop.BindableProperty
import matt.prim.str.elementsToString

class ByCategoryView(
  testLoader: TypedTestLike<*>,
  viewer: DatasetViewer,
  override val settings: DeephysSettingsController
): VBoxWrapperImpl<RegionWrapper<*>>(), DeephysNode {
  init {
	val categoryCB = choicebox(
	  nullableProp = BindableProperty(viewer.categorySelection.value),
	  values = testLoader.test.categories
	) {
	  converter = toStringConverter<CategorySelection?> {
		when (it) {
		  is Category          -> it.label
		  is CategoryConfusion -> it.allCategories.map { it.label }.toList().elementsToString()
		  else                 -> "no category selected"
		}
	  }.toFXConverter()
	  selectedItemProperty.onChangeWithWeak(viewer) { deRefedViewer, selection ->
		selection?.go {
		  deRefedViewer.navigateTo(it)
		}
	  }
	}
	deephysLabeledControl(
	  "Category",
	  categoryCB
	)
	swapper(
	  viewer.categorySelection,
	  nullMessage = "select a category",
	  fadeOutDur = DEEPHYS_FADE_DUR,
	  fadeInDur = DEEPHYS_FADE_DUR
	) {
	  CategoryView(this, testLoader = testLoader, viewer = viewer, settings = settings)
	}
  }
}