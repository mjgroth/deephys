package matt.nn.deephys.gui.dataset

import javafx.scene.input.MouseEvent
import matt.fx.control.wrapper.control.choice.choicebox
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByCategory
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephys.gui.dataset.DatasetNodeView.ByNeuron
import matt.nn.deephys.gui.dataset.bycategory.ByCategoryView
import matt.nn.deephys.gui.dataset.byimage.ByImageView
import matt.nn.deephys.gui.dataset.byneuron.ByNeuronView
import matt.nn.deephys.gui.dataset.dtab.DeephysTabPane
import matt.nn.deephys.gui.global.deephysLabeledControl2
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.load.test.TestLoader
import java.lang.ref.WeakReference

enum class DatasetNodeView {
  ByNeuron, ByImage, ByCategory
}

class DatasetNode(
  dataset: TestLoader,
  viewer: DatasetViewer,
  override val settings: DeephysSettingsController
): VBoxW(), DeephysNode {

  val weakViewer = WeakReference(viewer)

  private val weakSettings = WeakReference(settings)

  private val byNeuronView by lazy {
	ByNeuronView(
	  dataset,
	  viewer,
	  settings = weakSettings.get()!!
	)
  }
  private val byImageView by lazy { ByImageView(dataset.preppedTest.awaitRequireSuccessful(), viewer, settings = weakSettings.get()!!) }
  private val byCategoryView by lazy {
	ByCategoryView(
	  dataset.preppedTest.awaitRequireSuccessful(),
	  viewer,
	  settings = weakSettings.get()!!
	)
  }

  init {

	isFillWidth = false

	val layerCB = choicebox(
	  nullableProp = viewer.layerSelection,
	  values = viewer.model.resolvedLayers.map { it.interTest }
	) {
	  valueProperty.onChange {
		println("layerCB value changed to $it")
	  }

	}
	val layerController = deephysLabeledControl2(
	  "Layer",
	  layerCB
	) {
	  visibleAndManagedProp.bind(viewer.isUnboundToDSet)
	}

	+DeephysTabPane().apply {
	  layerController.prefWidthProperty.bind(this.tabBar.widthProperty)
	  val neuronTab = deephysLazyTab("Neuron") {
		this@DatasetNode.byNeuronView
	  }.apply {
		addEventFilter(MouseEvent.MOUSE_PRESSED) {
		  it.consume()
		  if (!this.isSelected) {
			this@DatasetNode.weakViewer.get()!!.navigateTo(
			  ByNeuron
			)
		  }
		}
	  }
	  val imageTab = deephysLazyTab("Image") {
		this@DatasetNode.byImageView
	  }.apply {
		addEventFilter(MouseEvent.MOUSE_PRESSED) {
		  it.consume()
		  if (!this.isSelected) {
			this@DatasetNode.weakViewer.get()!!.navigateTo(
			  ByImage
			)
		  }
		}
	  }
	  val categoryTab = deephysLazyTab("Category") {
		this@DatasetNode.byCategoryView
	  }.apply {
		addEventFilter(MouseEvent.MOUSE_PRESSED) {
		  it.consume()
		  if (!this.isSelected) {
			this@DatasetNode.weakViewer.get()!!.navigateTo(
			  ByCategory
			)
		  }
		}
	  }

	  fun update(view: DatasetNodeView) {
		when (view) {
		  ByNeuron -> neuronTab.fire()
		  ByImage -> imageTab.fire()
		  ByCategory -> categoryTab.fire()
		}
	  }
	  update(viewer.view.value)
	  viewer.view.onChange {
		update(it)
	  }
	}
  }
}