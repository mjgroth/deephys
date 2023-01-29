package matt.nn.deephys.gui.dataset.byimage

import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.hbox.HBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.lang.go
import matt.lang.weak.MyWeakRef
import matt.nn.deephys.calc.ImageTopPredictions
import matt.nn.deephys.gui.dataset.byimage.feat.FeaturesView
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.neuronListViewSwapper
import matt.nn.deephys.gui.dataset.byimage.preds.PredictionsView
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.testloadertwo.PreppedTestLoader
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.obs.bindings.bool.and


class ByImageView<A: Number>(
  testLoader: PreppedTestLoader<A>,
  viewer: DatasetViewer
): VBoxWrapperImpl<RegionWrapper<*>>() {
  init {
	val weakViewer = MyWeakRef(viewer)
	val weakTest = MyWeakRef(testLoader)
	deephyButton("select random image") {
	  setOnAction {
		weakViewer.deref()!!.imageSelection.value = weakTest.deref()!!.tl.awaitNonUniformRandomImage()
	  }
	  visibleAndManagedProp.bind(viewer.imageSelection.isNull.and(viewer.isUnboundToDSet))
	}
	swapper(
	  viewer.imageSelection, "no image selected",
	  fadeOutDur = DEEPHYS_FADE_DUR,
	  fadeInDur = DEEPHYS_FADE_DUR
	) {
	  val img = this@swapper
	  weakViewer.deref()?.let { deRefedViewer ->
		HBoxWrapperImpl<NodeWrapper>().apply {
		  +DeephyImView(img, deRefedViewer, big = true).apply {
			/*img.widthMaybe*/

			/*scale.bind(deRefedViewer.bigImageScale / img.widthMaybe)*/
			/*scale.value = 4.0*/
		  }
		  spacer(10.0)
		  @Suppress("UNCHECKED_CAST")
		  img as DeephyImage<A>
		  +PredictionsView(
			img.category, ImageTopPredictions(img, weakTest.deref()!!), weakViewer
		  )
		  spacer()
		  img.features?.takeIf { it.isNotEmpty() }?.go {
			+FeaturesView(it)
		  }
		}
	  } ?: TextWrapper("if you see this, then there must be a problem")

	}.apply {
	  visibleAndManagedProp.bind(viewer.isUnboundToDSet)
	}
	spacer(10.0)
	neuronListViewSwapper(
	  viewer = viewer,
	  top = viewer.topNeurons,
	  bindScrolling = true
	)
  }
}

