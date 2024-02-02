package matt.nn.deephys.gui.dataset.byimage.preds

import javafx.scene.Cursor
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.pane.hSpacer
import matt.fx.graphics.wrapper.pane.hbox.HBoxW
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.fx.graphics.wrapper.textflow.TextFlowWrapper
import matt.fx.node.proto.infosymbol.plusMinusSymbol
import matt.lang.weak.MyWeakRef
import matt.lang.weak.WeakRefInter
import matt.nn.deephys.calc.ImageTopPredictions
import matt.nn.deephys.gui.global.color.DeephysPalette
import matt.nn.deephys.gui.global.deephyActionLabel
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.sigFigText
import matt.nn.deephys.gui.global.subtitleFont
import matt.nn.deephys.gui.global.titleBoldFont
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.data.Category
import matt.obs.bindings.bool.not
import matt.obs.math.int.ObsI
import matt.obs.prop.BindableProperty

class PredictionsView(
    groundTruth: Category,
    topPreds: ImageTopPredictions<*>,
    weakViewer: MyWeakRef<DatasetViewer>,
    override val settings: DeephysSettingsController
) : VBoxW(), DeephysNode {
    init {
        val memSafeSettings = settings
        h {
            deephysText("Ground Truth: ").titleFont()
            deephyActionLabel(groundTruth.label) {
                weakViewer.deref()!!.navigateTo(groundTruth)
            }.titleBoldFont()
        }
        spacer()
        deephysText("Predictions: ").titleFont()
        with(weakViewer.deref()!!.testData.value!!.testRAMCache) {
            +CategoryTable(
                title = "",
                title_unfolded = "",
                data = topPreds().map { it.first to it.second },
                settings = memSafeSettings,
                weakViewer = weakViewer,
                sigFigSett = weakViewer.deref()!!.predictionSigFigs,
                tooltip = "Top classification layer output values. Numbers displayed have been run through a softmax."
            )
        }

    }
}

class CategoryTable(
    title: String,
    title_unfolded: String,
    data: List<Pair<Category, Number>>,
    weakViewer: WeakRefInter<DatasetViewer>,
    override val settings: DeephysSettingsController,
    tooltip: String,
    private val sigFigSett: ObsI,
    numSuffix: String = ""
) : HBoxW(), DeephysNode {




    init {
        val b = BindableProperty(false)
        plusMinusSymbol(b, radius = 6.5) {
            fill = DeephysPalette.deephysBlue2
            cursor = Cursor.HAND

        }
        hSpacer(5.0)
        v {
            +TextFlowWrapper<NW>().apply {
                visibleAndManagedProp.bindWeakly(b.not())
                deephysText(title_unfolded)
                data.forEach { (cat, num) ->
                    val fullString = "${cat.label} ($num)"
                    cat.actionText(
                        r = this,
                        tooltip = fullString,
                        settings = this@CategoryTable.settings,
                        weakViewer = weakViewer,
                        allowedLengths = 1..10
                    )
                    deephysText(" (")
                    sigFigText(
                        num = num,
                        sigFigSett = this@CategoryTable.sigFigSett,
                        numSuffix = numSuffix,
                        settings = this@CategoryTable.settings,
                        tooltip = fullString
                    )
                    deephysText(")   ")
                }
            }
            v {
                visibleAndManagedProp.bindWeakly(b)
                val memSafeWeakViewer = weakViewer
                val memSafeSettings = this@CategoryTable.settings
                deephysText(title) {
                    subtitleFont()
                    deephyTooltip(tooltip, settings = memSafeSettings)
                }
                spacer(1.0)
                v {
                    val predNamesBox = v {}
                    spacer(2.0)
                    val predValuesBox = v {}
                    h {
                        +predNamesBox
                        spacer()
                        +predValuesBox
                    }
                    data.forEach {
                        val category = it.first
                        val num = it.second

                        val fullString = "${category.label} ($num)"
                        category.actionText(
                            r = predNamesBox,
                            tooltip = fullString,
                            settings = memSafeSettings,
                            weakViewer = memSafeWeakViewer
                        )

                        predValuesBox.sigFigText(
                            num = num,
                            sigFigSett = this@CategoryTable.sigFigSett,
                            numSuffix = numSuffix,
                            settings = memSafeSettings,
                            tooltip = fullString
                        )
                    }
                }
            }
        }
    }
}
