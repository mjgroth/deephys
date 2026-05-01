package matt.nn.deephys.gui.global.tooltip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import matt.codegen.tex.TeXDSL
import matt.compose.components.tex.TeXView
import matt.compose.controls.tooltip.AreaWithTooltipInSupportedPlatforms
import matt.compose.graphics.ComposeContent
import matt.compose.graphics.border.defaultBorder
import matt.compose.graphics.image.desktop.MyImage
import matt.compose.graphics.text.MyText
import matt.compose.state.prop.rememberBoundComposeState
import matt.image.heavy.mutate.skiamutate.SkiaResize
import matt.lang.assertions.require.implementedFor
import matt.lang.function.Produce
import matt.nn.deephys.gui.draw.toSkiaImage
import matt.nn.deephys.gui.global.DEEPHYS_FONT_DEFAULT
import matt.nn.deephys.gui.global.DEEPHYS_LATEX_TOOLTIP_SCALE
import matt.nn.deephys.gui.global.color.DeephysPalette
import matt.nn.deephys.gui.settings.DEFAULT_BIG_IMAGE_SCALE
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.prim.pdouble.verifyWholeToInt

@Composable
fun DeephysTooltipArea(
    settings: DeephysSettingsController,
    getCode: Produce<TeXDSL>,
    content: ComposeContent
) {
    DeephysTooltipArea(
        settings = settings,
        tooltip = {
            TeXView(
                scale = DEEPHYS_LATEX_TOOLTIP_SCALE,
                code = getCode().generate()
            )
        },
        content = content
    )
}

@Suppress("unused")
@Composable
private fun DeephysTeXView(
    getCode: Produce<TeXDSL>
) {
    TeXView(
        scale = DEEPHYS_LATEX_TOOLTIP_SCALE,
        code = getCode().generate()
    )
}

@Composable
fun DeephysTooltipArea(
    settings: DeephysSettingsController,
    s: String,
    im: DeephyImage<*>? = null,
    enableTooltip: Boolean = true,
    modifier: Modifier = Modifier,
    content: ComposeContent
) {
    DeephysTooltipArea(
        modifier = modifier,
        settings = settings,
        enableTooltip = enableTooltip,
        tooltip = {
            Column {
                MyText(s, font = DEEPHYS_FONT_DEFAULT, modifier = Modifier.padding(10.dp))
                if (im != null) {
                    MyImage(
                        remember(im) {
                            val sIm = im.toSkiaImage()
                            SkiaResize(
                                h = (sIm.height * DEFAULT_BIG_IMAGE_SCALE).verifyWholeToInt(),
                                w = (sIm.width * DEFAULT_BIG_IMAGE_SCALE).verifyWholeToInt()
                            ).transform(sIm)
                        }
                    )
                }
            }
        },
        content = {
            content()
        }

    )
}

@Composable
private fun DeephysTooltipArea(
    tooltip: ComposeContent,
    settings: DeephysSettingsController,
    enableTooltip: Boolean = true,
    modifier: Modifier = Modifier,
    content: ComposeContent
) {
    implementedFor(settings.millisecondsBeforeTooltipsVanish.value == 0)
    AreaWithTooltipInSupportedPlatforms(
        tooltip = {
            DeephysTooltipContent(tooltip)
        },
        content = {
            content()
        },
        enableTooltip = enableTooltip,
        modifier = modifier
    )
}

@Composable
fun DeephysTooltipContent(
    content: ComposeContent
) {
    /*there was something with a white background here too in FX, but couldn't figure out what. An inner or outer box, maybe? Something for seeing the image or text correctly? I don't know. Could have been a mistake.*/
    Box(
        Modifier
            .background(
                DeephysPalette.tooltipBackground.rememberBoundComposeState().value
            )
            .defaultBorder(
                color = DeephysPalette.deephysBlue2
            )
    ) {
        content()
    }
}

const val SUFFIX_WARNING = "The `suffix` key is no longer supported (this can just be appended to the `name`). Please update to a newer version of the pip deephys package"
