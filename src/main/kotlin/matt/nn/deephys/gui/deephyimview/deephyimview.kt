package matt.nn.deephys.gui.deephyimview

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.asReadOnlyByteBuffer
import matt.compose.controls.interaction.rememberHoveredState
import matt.compose.controls.mouse.j.handPointerIcon
import matt.compose.graphics.color.ComposeColor
import matt.compose.graphics.color.toMcolor
import matt.compose.graphics.mods.thenIf
import matt.file.commons.reg.RegisteredFolder
import matt.image.desktop.save
import matt.model.k.log.Logger
import matt.model.k.log.warnPrefixed
import matt.nn.deephys.gui.draw.toSkiaImage
import matt.nn.deephys.gui.global.tooltip.DeephysTooltipArea
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.model.importformat.im.DeephyImage
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.awt.image.DataBufferInt

private var didWarnAboutCombiningMethods = false

@Suppress("UnusedParameter")
@OptIn(ExperimentalFoundationApi::class)
@Composable
context(_: Logger)
fun DeephyImView(
    im: DeephyImage<*>,
    viewer: DatasetViewerState,
    big: Boolean = false,
    loadAsync: Boolean = false,
    settings: DeephysSettingsController
) {

    val weakViewer = viewer.weakRef
    val weakIm = im.weak
    if (!didWarnAboutCombiningMethods) {
        warnPrefixed("combine draw methods for V1 and deephy")
        didWarnAboutCombiningMethods = true
    }
    val (interactionSource, isHovered) = rememberHoveredState()
    ContextMenuArea(
        items = {
            buildList {
                add(
                    ContextMenuItem(
                        "download image"
                    ) {
                        warnPrefixed(
                            """
                                  saveFile(stage = weakThis.get()!!.stage) {
                                title = "choose where to save png"
                                extensionFilter(
                                    description = "png",
                                    FileExtension.PNG
                                )
                                initialSaveFileName =
                                    localWeakIm.deref()!!.category.label + "_" + localWeakIm.deref()!!.index.toString() + ".png"
                            }
                            """.trimIndent()
                        )
                        val pngFile = RegisteredFolder.Main.tempPath("TEMP_PNG")

                        @Suppress("SENSELESS_COMPARISON")
                        if (pngFile != null) {

                            val mat2 = weakIm.deref()!!.matrix
                            val bi = BufferedImage(mat2.size, mat2[0].size, TYPE_INT_ARGB)
                            val pixelData = (bi.raster.dataBuffer as DataBufferInt).data

                            println("bi.width = ${bi.width}")
                            println("bi.height = ${bi.height}")
                            println("pixelData.length = ${pixelData.size}")

                            var i = 0
                            mat2.forEach { colorMutableList ->
                                colorMutableList.forEach {
                                    val awt = it.toMcolor().roundToRGBA()
                                    pixelData[i++] =
                                        ByteString(
                                            awt.alpha.toByte(),
                                            awt.red.toByte(),
                                            awt.green.toByte(),
                                            awt.blue.toByte()
                                        )
                                            .asReadOnlyByteBuffer()
                                            .asIntBuffer()
                                            .get()
                                }
                            }

                            require(pngFile.path.endsWith(".png"))
                            bi.save(pngFile)
                        }
                    }
                )
            }
        }
    ) {
        DeephysTooltipArea(
            settings,
            weakIm.deref()!!.category.label,
            weakIm.deref()
        ) {
            Box(
                Modifier.thenIf(
                    isHovered,
                    Modifier.border(
                        width = 1.dp,
                        color = ComposeColor.Yellow
                    )
                )
            ) {
                matt.compose.graphics.image.desktop.MyImage(
                    im.toSkiaImage(),
                    modifier =
                        Modifier
                            .onClick {
                                weakViewer.deref()!!.navigateTo(weakIm.deref()!!)
                            }
                            .handPointerIcon()
                            .scale(
                                run {
                                    val widthMaybe = im.toSkiaImage().width
                                    if (big) {
                                        (weakViewer.deref()!!.bigImageScale.value / widthMaybe).toFloat()
                                    } else {
                                        (weakViewer.deref()!!.smallImageScale.value / widthMaybe).toFloat()
                                    }
                                }
                            )
                            .hoverable(interactionSource)
                )
            }
        }
    }
}
