package matt.nn.deephys.gui.dataset.byimage.preds

import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.hbox
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.fx.graphics.wrapper.pane.vbox.vbox
import matt.fx.graphics.wrapper.text.TextWrapper
import matt.fx.graphics.wrapper.textflow.textflow
import matt.lang.weak.MyWeakRef
import matt.math.jmath.sigFigs
import matt.nn.deephys.calc.ImageTopPredictions
import matt.nn.deephys.gui.global.deephyActionText
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.data.Category
import matt.obs.bind.binding
import matt.prim.str.truncateWithElipsesOrAddSpaces

class PredictionsView(
  groundTruth: Category,
  topPreds: ImageTopPredictions<*>,
  weakViewer: MyWeakRef<DatasetViewer>,
  override val settings: DeephysSettingsController
): VBoxW(), DeephysNode {
  init {
	val memSafeSettings = settings
	textflow<TextWrapper> {
	  deephysText("Ground Truth: ").titleFont()
	  deephyActionText(groundTruth.label) {
		weakViewer.deref()!!.navigateTo(groundTruth)
	  }.titleBoldFont()
	}
	spacer()
	deephysText("Predictions:") {
	  subtitleFont()
	}
	spacer(10.0)
	v {

	  val predNamesBox: NodeWrapper = vbox<TextWrapper> {}
	  spacer()
	  val predValuesBox: NodeWrapper = vbox<TextWrapper> {}
	  hbox {
		+predNamesBox
		spacer()
		+predValuesBox
	  }
	  topPreds().forEach {
		val category = it.first
		val pred = it.second
		val fullString = "\t${category.label} (${pred})"
		predNamesBox.deephyActionText(category.label.truncateWithElipsesOrAddSpaces(25)) {
		  weakViewer.deref()!!.navigateTo(category)
		}.apply {
		  veryLazyDeephysTooltip(fullString, memSafeSettings)
		}
		predValuesBox.deephysText {
		  textProperty.bind(weakViewer.deref()!!.predictionSigFigs.binding {
			when (pred) {
			  is Float  -> pred.sigFigs(it).toString()
			  is Double -> pred.sigFigs(it).toString()
			  else      -> error("not ready for different dtype")
			}

		  })
		  veryLazyDeephysTooltip(fullString, memSafeSettings)
		}
	  }
	}
  }
}