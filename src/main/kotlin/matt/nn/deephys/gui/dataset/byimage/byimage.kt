package matt.nn.deephys.gui.dataset.byimage

import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperR
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.lang.go
import matt.lang.weak.MyWeakRef
import matt.lang.weak.WeakRefInter
import matt.nn.deephys.calc.ImageTopPredictions
import matt.nn.deephys.gui.dataset.MainDeephysView
import matt.nn.deephys.gui.dataset.byimage.feat.FeaturesView
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.neuronListViewSwapper
import matt.nn.deephys.gui.dataset.byimage.preds.PredictionsView
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.deephysSpinner
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.testloadertwo.PreppedTestLoader
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal


class ByImageView<A: Number>(
  testLoader: PreppedTestLoader<A>,
  viewer: DatasetViewer,
  override val settings: DeephysSettingsController
): VBoxWrapperImpl<RegionWrapper<*>>(), MainDeephysView {


  override var control: ObsVal<WeakRefInter<RegionWrapper<*>>?> = BindableProperty(null)
	private set

  init {
	val memSafeSettings = settings
	val weakViewer = MyWeakRef(viewer)
	val weakTest = MyWeakRef(testLoader)

	val images = testLoader.test.images

	deephysSpinner(
	  label = "Image",
	  choices = images,
	  defaultChoice = { images[0] },
	  converter = DeephyImage.stringConverterThatFallsBackToFirst(images = images),
	  viewer = viewer,
	  getCurrent = viewer.imageSelection,
	  acceptIf = { true },
	  navAction = { navigateTo(it) }
	).apply {
	  (this@ByImageView.control as BindableProperty<WeakRefInter<RegionWrapper<*>>?>).value = MyWeakRef(this.first)
	}

	swapperR(
	  viewer.imageSelection,
	  "no image selected",
	  fadeOutDur = DEEPHYS_FADE_DUR,
	  fadeInDur = DEEPHYS_FADE_DUR
	) { img ->

	  weakViewer.deref()?.let { deRefedViewer ->


		h {
		  v {
			+DeephyImView(img, deRefedViewer, big = true, settings = memSafeSettings)
		  }
		  spacer(10.0)
		  @Suppress("UNCHECKED_CAST")
		  img as DeephyImage<A>
		  +PredictionsView(
			img.category,
			ImageTopPredictions(img, weakTest.deref()!!),
			weakViewer,
			memSafeSettings
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
	  bindScrolling = true,
	  settings = memSafeSettings
	)
  }


}

