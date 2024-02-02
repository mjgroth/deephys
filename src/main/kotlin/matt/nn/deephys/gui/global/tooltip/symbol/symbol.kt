package matt.nn.deephys.gui.global.tooltip.symbol

import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.attachTo
import matt.fx.node.proto.infosymbol.InfoSymbol
import matt.fx.node.proto.infosymbol.SevereWarningSymbol
import matt.fx.node.proto.infosymbol.TutorialSymbol
import matt.fx.node.proto.infosymbol.WarningSymbol
import matt.lang.function.Dsl
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipContent
import matt.obs.bindings.str.ObsS


const val DEEPHYS_SYMBOL_SPACING = 5.0

fun NW.deephysInfoSymbol(text: ObsS, op: Dsl<DeephysInfoSymbol> = {}) = DeephysInfoSymbol(text.value).attachTo(this) {
    textProperty.bind(text)
    op()
}

fun NW.deephysInfoSymbol(text: String, op: Dsl<DeephysInfoSymbol> = {}) = DeephysInfoSymbol(text).attachTo(this, op)


class DeephysInfoSymbol(info: String): InfoSymbol(info) {
    override fun buildTooltipGraphic(text: String) = DeephysTooltipContent(text)
    val textProperty get() = (content as DeephysTooltipContent).theLabel.textProperty
    val fontProperty get() = (content as DeephysTooltipContent).theLabel.fontProperty
}


fun NW.deephysTutorialSymbol(text: ObsS, op: Dsl<DeephysTutorialSymbol> = {}) = DeephysTutorialSymbol(text.value).attachTo(this) {
    textProperty.bind(text)
    op()
}

fun NW.deephysTutorialSymbol(text: String, op: Dsl<DeephysTutorialSymbol> = {}) = DeephysTutorialSymbol(text).attachTo(this, op)


class DeephysTutorialSymbol(info: String): TutorialSymbol(info) {
    override fun buildTooltipGraphic(text: String) = DeephysTooltipContent(text)
    val textProperty get() = (content as DeephysTooltipContent).theLabel.textProperty
    val fontProperty get() = (content as DeephysTooltipContent).theLabel.fontProperty
}


fun NW.deephysWarningSymbol(text: ObsS, op: Dsl<DeephysWarningSymbol> = {}) =
    DeephysWarningSymbol(text.value).attachTo(this) {
        textProperty.bind(text)
        op()
    }

fun NW.deephysWarningSymbol(text: String, op: Dsl<DeephysWarningSymbol> = {}) =
    DeephysWarningSymbol(text).attachTo(this, op)


class DeephysWarningSymbol(warning: String): WarningSymbol(warning) {
    override fun buildTooltipGraphic(text: String) = DeephysTooltipContent(text)
    val textProperty get() = (content as DeephysTooltipContent).theLabel.textProperty
    val fontProperty get() = (content as DeephysTooltipContent).theLabel.fontProperty
}



fun NW.deephysSevereWarningSymbol(text: ObsS, op: Dsl<DeephysSevereWarningSymbol> = {}) =
    DeephysSevereWarningSymbol(text.value).attachTo(this) {
        textProperty.bind(text)
        op()
    }

fun NW.deephysSevereWarningSymbol(text: String, op: Dsl<DeephysSevereWarningSymbol> = {}) =
    DeephysSevereWarningSymbol(text).attachTo(this, op)


class DeephysSevereWarningSymbol(warning: String): SevereWarningSymbol(warning) {
    override fun buildTooltipGraphic(text: String) = DeephysTooltipContent(text)
    val textProperty get() = (content as DeephysTooltipContent).theLabel.textProperty
    val fontProperty get() = (content as DeephysTooltipContent).theLabel.fontProperty
}
