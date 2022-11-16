package matt.nn.deephys.gui.dataset.byimage

import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.PaneWrapper
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.hbox.HBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.fx.graphics.wrapper.textflow.textflow
import matt.lang.go
import matt.lang.weak.WeakRef
import matt.math.jmath.sigFigs
import matt.nn.deephys.calc.ImageTopPredictions
import matt.nn.deephys.calc.UniqueContents
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.neuronListViewSwapper
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.deephyActionText
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.importformat.DeephyImage
import matt.obs.bind.binding
import matt.obs.bindings.bool.and
import matt.obs.bindings.bool.or
import matt.obs.math.op.times
import matt.prim.str.truncateWithElipsesOrAddSpaces


class MultipleImagesView(
  viewer: DatasetViewer,
  images: List<DeephyImage>,
  title: String,
  tooltip: String
): VBoxWrapperImpl<NW>() {
  companion object {
	private const val MAX_IMS = 25
  }

  init {
	deephyText("$title (${images.size})").apply {
	  subtitleFont()
	  deephyTooltip("$tooltip (first $MAX_IMS)")
	}
	+ImageFlowPane(viewer).apply {
	  prefWrapLengthProperty.bind(viewer.widthProperty*0.4)
	  images.take(MAX_IMS).forEach {
		+DeephyImView(it, viewer)
	  }
	}
	neuronListViewSwapper(
	  viewer = viewer,
	  contents = UniqueContents(images)
	)
  }
}

class ByImageView(
  testLoader: TestLoader, viewer: DatasetViewer
): VBoxWrapperImpl<RegionWrapper<*>>() {
  init {
	val weakViewer = WeakRef(viewer)
	val weakTest = WeakRef(testLoader)
	deephyButton("select random image") {
	  setOnAction {
		weakViewer.deref()!!.imageSelection.value = weakTest.deref()!!.awaitNonUniformRandomImage()
	  }
	  visibleAndManagedProp.bind(viewer.imageSelection.isNull.and(viewer.isUnboundToDSet))
	}
	swapper(viewer.imageSelection, "no image selected") {
	  val img = this@swapper
	  weakViewer.deref()?.let { deRefedViewer ->
		HBoxWrapperImpl<NodeWrapper>().apply {
		  +DeephyImView(img, deRefedViewer).apply {
			scale.value = 4.0
		  }
		  spacer(10.0)
		  vbox {
			textflow<TextWrapper> {
			  deephyText("ground truth: ").titleFont()
			  val cat = img.category
			  deephyActionText(cat.label) {
				weakViewer.deref()!!.navigateTo(cat)
			  }.titleBoldFont()
			}
			spacer()

			deephyText("predictions:") {
			  subtitleFont()
			}
			spacer(10.0)
			val predNamesBox: NodeWrapper = vbox<TextWrapper> {}
			val predValuesBox: NodeWrapper = vbox<TextWrapper> {}
			hbox<PaneWrapper<*>> {
			  +predNamesBox
			  spacer()
			  +predValuesBox
			}
			val topPreds = ImageTopPredictions(img, weakTest.deref()!!)()
			topPreds.forEach {
			  val category = it.first
			  val pred = it.second
			  val fullString = "\t${category.label} (${pred})"
			  predNamesBox.deephyActionText(category.label.truncateWithElipsesOrAddSpaces(25)) {
				weakViewer.deref()!!.navigateTo(category)
			  }.apply {
				deephyTooltip(fullString)
			  }
			  predValuesBox.deephyText {
				textProperty.bind(deRefedViewer.predictionSigFigs.binding {
				  pred.sigFigs(it).toString()
				})
				deephyTooltip(fullString)
			  }
			}
		  }
		  spacer()
		  img.features?.takeIf { it.isNotEmpty() }?.go {
			vbox {
			  deephyText("Features:") {
				subtitleFont()
			  }
			  spacer()
			  val featureKeysBox: NodeWrapper = vbox<TextWrapper> {}
			  val featureValuesBox: NodeWrapper = vbox<TextWrapper> {}
			  hbox<PaneWrapper<*>> {
				+featureKeysBox
				spacer()
				+featureValuesBox
			  }
			  it.forEach { (k, v) ->
				featureKeysBox.deephyText(k.truncateWithElipsesOrAddSpaces(25))
				featureValuesBox.deephyText(v)
			  }
			}
		  }

		}
	  } ?: TextWrapper("if you see this, then there must be a problem")

	}.apply {
	  visibleAndManagedProp.bind(viewer.isUnboundToDSet)
	}
	spacer(10.0)

	neuronListViewSwapper(
	  viewer = viewer,
	  top = viewer.topNeurons
	).apply {
	  visibleAndManagedProp.bind(viewer.isBoundToDSet.or(viewer.imageSelection.isNotNull))
	}
  }
}

