package matt.nn.deephys.gui.neuron

import matt.async.queue.QueueWorker
import matt.collect.itr.subList
import matt.fx.base.prop.sizeProperty
import matt.fx.control.wrapper.progressindicator.progressindicator
import matt.fx.graphics.fxthread.ensureInFXThreadOrRunLater
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperNullable
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperRNullable
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.node.tex.dsl.tex
import matt.lang.NEVER
import matt.lang.function.Consume
import matt.lang.go
import matt.math.op.div
import matt.model.flowlogic.await.Donable
import matt.nn.deephys.calc.ActivationRatioCalc
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.MiscActivationRatioNumerator
import matt.nn.deephys.calc.TopCategories
import matt.nn.deephys.calc.TopImages
import matt.nn.deephys.calc.act.ActivationRatio
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.NeuronListView
import matt.nn.deephys.gui.dataset.byimage.preds.CategoryTable
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.tooltip.symbol.deephysInfoSymbol
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTexTooltip
import matt.nn.deephys.gui.neuron.imgflowpane.ImageFlowPane
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.settings.MAX_NUM_IMAGES_IN_TOP_IMAGES
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.data.ImageIndex
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.obs.bind.weakBinding
import matt.obs.bindings.bool.and
import matt.obs.bindings.bool.not
import matt.obs.math.double.op.times
import matt.obs.prop.BindableProperty
import matt.reflect.weak.WeakThing
import kotlin.math.min

class NeuronView<A: Number>(
  neuron: InterTestNeuron,
  numImages: BindableProperty<Int> = BindableProperty(MAX_NUM_IMAGES_IN_TOP_IMAGES),
  testLoader: TypedTestLike<A>,
  viewer: DatasetViewer,
  showActivationRatio: Boolean,
  layoutForList: Boolean,
  loadImagesAsync: Boolean = false,
  showTopCats: Boolean = false,
  override val settings: DeephysSettingsController
): VBoxWrapperImpl<NW>(), DeephysNode {

  companion object {
	private val worker = QueueWorker("NeuronView Worker")
  }


  init {
	val memSafeSettings = settings
	val weakViewer = viewer.weakRef
	val showing = BindableProperty(2)
	val progIndicator = progressindicator {
	  visibleAndManagedProp.bindWeakly(showing.weakBinding(this@NeuronView) { _, it ->
		it < 2
	  })
	}

	if (showActivationRatio) {
	  swapperRNullable(viewer.normalizer) { normalizer ->
		weakViewer.deref()!!.testData.value?.go { numTest ->
		  val denomTest = normalizer?.testData?.value
		  h {
			val doneLoading = numTest.isDoneLoading() && (denomTest?.isDoneLoading() ?: true)

			if (!doneLoading) showing.value -= 1

			@Suppress("UNCHECKED_CAST")
			val j = worker.scheduleOrRunSynchroneouslyIf(doneLoading) {
			  denomTest?.let {
				neuron.activationRatio(
				  numTest = numTest.preppedTest.awaitRequireSuccessful() as TypedTestLike<A>,
				  denomTest = denomTest.preppedTest.awaitRequireSuccessful() as TypedTestLike<A>,
				)
			  } ?: neuron.maxActivationIn(
				test = numTest.preppedTest.awaitRequireSuccessful() as TypedTestLike<A>,
			  )
			}

			j.whenDone { activation ->
			  ensureInFXThreadOrRunLater {
				if (!doneLoading) {
				  showing.value += 1
				}
				deephysText(
				  activation.formatted
				) {
				  veryLazyDeephysTexTooltip(memSafeSettings) {

					when (activation) {
					  is ActivationRatio -> ActivationRatioCalc.latexTechnique(MiscActivationRatioNumerator.MAX)
					  is RawActivation   -> tex { text("max raw activation of this neuron") }
					  else               -> ActivationRatioCalc.latexTechnique(MiscActivationRatioNumerator.MAX)
					}


				  }
				}
				activation.extraInfo?.go { deephysInfoSymbol(it) }
			  }
			}
		  }
		}
	  }
	}

	if (showTopCats) {
	  val topCats = TopCategories(neuron, testLoader)()


	  val dtype = testLoader.dtype

	  swapperNullable(viewer.normalizer) {
		val normalizer = this?.testData?.value?.preppedTest?.awaitRequireSuccessful()
		val denom = normalizer?.let {
		  neuron.maxActivationIn(normalizer).value/100
		} ?: dtype.one

		val normalizedString = if (normalizer == null) "un-normalized" else "normalized"

		CategoryTable(
		  title = "Average activity for top categories: ",
		  title_unfolded = "ave: ",
		  data = topCats.map { it.first to (it.second.value/denom) },
		  settings = memSafeSettings,
		  weakViewer = weakViewer,
		  sigFigSett = weakViewer.deref()!!.averageRawActSigFigs,
		  tooltip = "Top categories for this neuron. Calculated by the average, $normalizedString activation of this neuron for images grouped by their groundtruth",
		  numSuffix = if (normalizer == null) "" else "%"
		)
	  }


	  spacer(1.0)
	}


	val noneText = deephysInfoSymbol(
	  "There are no top images. This might happen if all activations are zero, NaN, or infinite"
	)
	+ImageFlowPane(viewer).apply {
	  noneText.visibleAndManagedProp.bindWeakly(
		children.sizeProperty.eq(0) and progIndicator.visibleProperty.not()
	  )
	  val imFlowPane = this
	  /*for reasons that I don't understand, without this this FlowPane gets really over-sized in the y dimension*/
	  prefWrapLengthProperty.bindWeakly(viewer.widthProperty*0.95)

	  fun update(
		weakThing: WeakNeuronViewRefs<A>,
		oldNumImages: Int?,
		newNumImages: Int
	  ) {
		val localTestLoader = weakThing.testLoader
		val localViewer = weakThing.viewer
		val localNeuron = weakThing.neuron
		val localImFlowPane = weakThing.imFlowPane

		val realOldNumImages = oldNumImages?.let { min(it.toULong(), localTestLoader.numberOfImages()) }?.toULong()
		val realNumImages = min(newNumImages.toULong(), localTestLoader.numberOfImages())

		val doneLoading = localTestLoader.isDoneLoading()

		val topImagesJob = if (localTestLoader.isDoneLoading()) {
		  val ti = TopImages(localNeuron, localTestLoader, realNumImages.toInt())()
		  object: Donable<List<ImageIndex>> {
			override fun whenDone(c: Consume<List<ImageIndex>>) {
			  c(ti)
			}
		  }
		} else {
		  showing.value -= 1
		  worker.schedule {
			TopImages(localNeuron, localTestLoader, realNumImages.toInt())()
		  }
		}
		topImagesJob.whenDone { topImages ->
		  ensureInFXThreadOrRunLater {

			//			if (localNeuron.index == 10) {
			//			  taball("neuron 10 images", topImages)
			//			}

			if (realOldNumImages == null) {
			  topImages.forEach {
				val im = localTestLoader.imageAtIndex(it.index)
				localImFlowPane.add(
				  DeephyImView(
					im,
					localViewer,
					loadAsync = loadImagesAsync,
					settings = memSafeSettings
				  )
				)
			  }
			} else if (realNumImages > realOldNumImages) {
			  topImages.subList(realOldNumImages.toInt()).toList().forEach {
				val im = localTestLoader.imageAtIndex(it.index)
				localImFlowPane.add(
				  DeephyImView(
					im,
					localViewer,
					loadAsync = loadImagesAsync,
					settings = memSafeSettings
				  )
				)
			  }
			} else if (realNumImages < realOldNumImages) {
			  localImFlowPane.children.subList(realNumImages.toInt()).toList().forEach {
				it.removeFromParent()
			  }
			}
			if (!doneLoading) {
			  showing.value += 1
			}
		  }


		}


	  }


	  val weakThing = WeakNeuronViewRefs<A>().apply {
		this.testLoader = testLoader
		this.viewer = viewer
		this.neuron = neuron
		this.imFlowPane = imFlowPane
	  }


	  update(weakThing.deref()!!, null, numImages.value)

	  numImages.onChangeWithAlreadyWeakAndOld(weakThing) { tl, o, n ->
		update(tl, o, n)
	  }
	  if (layoutForList) {
		prefWrapLength = NeuronListView.NEURON_LIST_VIEW_WIDTH
	  }
	  val gap = 3.0
	  hgap = gap
	  vgap = gap
	}
  }


}

private class WeakNeuronViewRefs<A: Number>: WeakThing<WeakNeuronViewRefs<A>>() {

  var testLoader by weak<TypedTestLike<A>>()
  var viewer by weak<DatasetViewer>()
  var neuron by weak<InterTestNeuron>()
  var imFlowPane by weak<ImageFlowPane>()

}



