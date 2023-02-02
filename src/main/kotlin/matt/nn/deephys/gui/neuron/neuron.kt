package matt.nn.deephys.gui.neuron

import matt.async.queue.QueueWorker
import matt.collect.itr.subList
import matt.collect.set.contents.contentsOf
import matt.fx.control.wrapper.progressindicator.progressindicator
import matt.fx.graphics.fxthread.ensureInFXThreadOrRunLater
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapperR
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.node.proto.infosymbol.infoSymbol
import matt.fx.node.proto.scaledcanvas.toCanvas
import matt.fx.node.tex.texToPixels
import matt.hurricanefx.eye.prop.sizeProperty
import matt.lang.function.Consume
import matt.lang.go
import matt.model.flowlogic.await.Donable
import matt.nn.deephys.calc.ActivationRatioCalc
import matt.nn.deephys.calc.ActivationRatioCalc.Companion.MiscActivationRatioNumerator
import matt.nn.deephys.calc.TopImages
import matt.nn.deephys.calc.act.Activation
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.NeuronListView
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.deephyText
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltipWithNode
import matt.nn.deephys.gui.neuron.imgflowpane.ImageFlowPane
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
  numImages: BindableProperty<Int> = BindableProperty(100),
  testLoader: TypedTestLike<A>,
  viewer: DatasetViewer,
  showActivationRatio: Boolean,
  layoutForList: Boolean,
  loadImagesAsync: Boolean = false
): VBoxWrapperImpl<NW>() {

  companion object {
	private val worker = QueueWorker("NeuronView Worker")
  }


  init {
	val weakViewer = viewer.weakRef
	val showing = BindableProperty(2)
	val progIndicator = progressindicator {
	  visibleAndManagedProp.bindWeakly(showing.weakBinding(this@NeuronView) { _, it ->
		it < 2
	  })
	}

	if (showActivationRatio) {
	  swapperR(viewer.normalizer) {
		weakViewer.deref()!!.testData.value?.go { numTest ->
		  it.testData.value?.go { denomTest ->
			h {


			  val doneLoading = denomTest.isDoneLoading() && numTest.isDoneLoading()

			  @Suppress("UNCHECKED_CAST")
			  val j = if (doneLoading) {
				val ti = ActivationRatioCalc(
				  numTest = numTest.preppedTest.await() as TypedTestLike<A>,
				  images = contentsOf(),
				  denomTest = denomTest.preppedTest.await() as TypedTestLike<A>,
				  neuron = neuron
				)()
				object: Donable<Activation<A, *>> {
				  override fun whenDone(c: Consume<Activation<A, *>>) {
					c(ti)
				  }
				}
			  } else {
				showing.value -= 1
				worker.schedule {
				  ActivationRatioCalc(
					numTest = numTest.preppedTest.await() as TypedTestLike<A>,
					images = contentsOf(),
					denomTest = denomTest.preppedTest.await() as TypedTestLike<A>,
					neuron = neuron
				  )()
				}
			  }

			  j.whenDone { ratio ->
				ensureInFXThreadOrRunLater {
				  if (!doneLoading) {
					showing.value += 1
				  }
				  deephyText(
					ratio.formatted
				  ) {
					veryLazyDeephysTooltipWithNode {
					  ActivationRatioCalc.latexTechnique(MiscActivationRatioNumerator.MAX).texToPixels()!!.toCanvas()
					}
					//					veryLazyDeephysTooltip(ActivationRatioCalc.technique)
				  }
				  //			  infoSymbol("test")
				  ratio.extraInfo?.go { infoSymbol(it) }

				}
			  }


			}
		  }
		}
	  }
	}

	val noneText = infoSymbol(
	  "There are no top images. This might happen if all activations are zero, NaN, or infinite"
	)
	+ImageFlowPane(viewer).apply {
	  noneText.visibleAndManagedProp.bindWeakly(
		children.sizeProperty.eq(0) and progIndicator.visibleProperty.not()
	  )
	  val imFlowPane = this
	  /*for reasons that I don't understand, without this this FlowPane gets really over-sized in the y dimension*/
	  prefWrapLengthProperty.bindWeakly(viewer.widthProperty*0.8)

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
				localImFlowPane.add(DeephyImView(im, localViewer, loadAsync = loadImagesAsync))
			  }
			} else if (realNumImages > realOldNumImages) {
			  topImages.subList(realOldNumImages.toInt()).toList().forEach {
				val im = localTestLoader.imageAtIndex(it.index)
				localImFlowPane.add(DeephyImView(im, localViewer, loadAsync = loadImagesAsync))
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
		hgap = 10.0
		vgap = 10.0
	  }
	}
  }


}

private class WeakNeuronViewRefs<A: Number>: WeakThing<WeakNeuronViewRefs<A>>() {

  var testLoader by weak<TypedTestLike<A>>()
  var viewer by weak<DatasetViewer>()
  var neuron by weak<InterTestNeuron>()
  var imFlowPane by weak<ImageFlowPane>()

}



