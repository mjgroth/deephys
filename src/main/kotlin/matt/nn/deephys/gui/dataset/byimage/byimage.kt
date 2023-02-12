package matt.nn.deephys.gui.dataset.byimage

import javafx.scene.paint.Color
import matt.fx.control.wrapper.control.spinner.spinner
import matt.fx.graphics.style.border.FXBorder
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperR
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.v
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
import matt.nn.deephys.gui.global.deephysLabeledControl
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.testloadertwo.PreppedTestLoader
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.obs.bind.binding
import matt.obs.bindings.bool.ObsB
import matt.obs.bindings.bool.and
import matt.obs.col.olist.toBasicObservableList
import matt.prim.str.isInt


class ByImageView<A: Number>(
  testLoader: PreppedTestLoader<A>,
  viewer: DatasetViewer,
  override val settings: DeephysSettingsController
): VBoxWrapperImpl<RegionWrapper<*>>(), DeephysNode {



  init {
	val memSafeSettings = settings
	val weakViewer = MyWeakRef(viewer)
	val weakTest = MyWeakRef(testLoader)

	var badText: ObsB? = null
	val images = testLoader.test.images
	val firstImage = images[0]
	val imageSpinner = spinner(
	  items = images.toBasicObservableList(),
	  editable = true,
	  enableScroll = false,
	  converter = DeephyImage.stringConverterThatFallsBackToFirst(images = images)
	) {

	  autoCommitOnType()

	  valueFactory!!.wrapAround = true

	  badText = textProperty.binding {
		it == null || !it.isInt() || it.toInt() !in images.indices
	  }

	  badText!!.onChange {
		border = if (it) FXBorder.solid(Color.RED, 10.0)
		else null
	  }

	  val image = viewer.imageSelection.value ?: firstImage
	  valueFactory!!.value = image
	  viewer.imageSelection v image

	  bindBidirectional(
		viewer.imageSelection,
		default = firstImage,
		acceptIf = {
		  true
		}
	  )

	  visibleAndManagedProp.bind(viewer.isUnboundToDSet)

	}

	deephysText("please input valid integer image index between 0 and ${images.size}") {
	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull.and(badText!!))
	}

	deephysLabeledControl("Image", imageSpinner) {
	  visibleAndManagedProp.bind(viewer.boundToDSet.isNull)
	}

	/*
		deephyButton("select random image") {
		  setOnAction {
			weakViewer.deref()!!.imageSelection.value = weakTest.deref()!!.tl.awaitNonUniformRandomImage()
		  }
		  visibleAndManagedProp.bind(viewer.imageSelection.isNull.and(viewer.isUnboundToDSet))
		}*/





	println("viewer.imageSelection.value1  = ${viewer.imageSelection.value}")

	swapperR(
	  viewer.imageSelection,
	  "no image selected",
	  fadeOutDur = DEEPHYS_FADE_DUR,
	  fadeInDur = DEEPHYS_FADE_DUR
	) { img ->
	  println("viewer.imageSelection.value2  = ${viewer.imageSelection.value}")
	  println("image swapper img: $img")

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

