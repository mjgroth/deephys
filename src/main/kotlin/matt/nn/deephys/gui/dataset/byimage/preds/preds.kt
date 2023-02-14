package matt.nn.deephys.gui.dataset.byimage.preds

import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.lang.weak.MyWeakRef
import matt.lang.weak.WeakRefInter
import matt.math.jmath.sigFigs
import matt.nn.deephys.calc.ImageTopPredictions
import matt.nn.deephys.gui.global.deephyActionLabel
import matt.nn.deephys.gui.global.deephyActionText
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.data.Category
import matt.obs.bind.weakBinding
import matt.obs.math.int.ObsI
import matt.prim.str.truncateWithElipsesOrAddSpaces

class PredictionsView(
  groundTruth: Category,
  topPreds: ImageTopPredictions<*>,
  weakViewer: MyWeakRef<DatasetViewer>,
  override val settings: DeephysSettingsController
): VBoxW(), DeephysNode {
  init {
	val memSafeSettings = settings
	h {
	  deephysText("Ground Truth: ").titleFont()
	  deephyActionLabel(groundTruth.label) {
		weakViewer.deref()!!.navigateTo(groundTruth)
	  }.titleBoldFont()
	}
	spacer()
	+CategoryTable(
	  title = "Predictions:",
	  data = topPreds().map { it.first to it.second },
	  settings = memSafeSettings,
	  weakViewer = weakViewer,
	  sigFigSett = weakViewer.deref()!!.predictionSigFigs,
	  tooltip = "Top classification layer output values. Numbers displayed have been run through a softmax."
	)
  }
}


class CategoryTable(
  title: String,
  data: List<Pair<Category, Number>>,
  weakViewer: WeakRefInter<DatasetViewer>,
  override val settings: DeephysSettingsController,
  tooltip: String,
  private val sigFigSett: ObsI,
  numSuffix: String = ""
): VBoxW(), DeephysNode {
  init {
	val memSafeWeakViewer = weakViewer
	val memSafeSettings = settings
	deephysText(title) {
	  subtitleFont()
	  deephyTooltip(tooltip, settings = memSafeSettings)
	}
	spacer(5.0)
	v {

	  val predNamesBox = v {}
	  spacer()
	  val predValuesBox = v {}
	  hbox {
		+predNamesBox
		spacer()
		+predValuesBox
	  }
	  data.forEach {
		val category = it.first
		val num = it.second
		val fullString = "\t${category.label} (${num})"
		predNamesBox.deephyActionText(category.label.truncateWithElipsesOrAddSpaces(25)) {
		  memSafeWeakViewer.deref()!!.navigateTo(category)
		}.apply {
		  veryLazyDeephysTooltip(fullString, memSafeSettings)
		}
		predValuesBox.deephysText {
		  textProperty.bindWeakly(this@CategoryTable.sigFigSett.weakBinding(this) { _, it ->
			when (num) {
			  is Float  -> num.sigFigs(it).toString()
			  is Double -> num.sigFigs(it).toString()
			  else      -> error("not ready for different dtype")
			} + numSuffix
		  })
		  veryLazyDeephysTooltip(fullString, memSafeSettings)
		}
	  }
	}
  }
}