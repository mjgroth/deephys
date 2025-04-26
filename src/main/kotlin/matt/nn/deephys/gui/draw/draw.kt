package matt.nn.deephys.gui.draw

import matt.compose.graphics.color.toMcolor
import matt.image.heavy.skikoutil.allocPixelsAndCheck
import matt.image.heavy.skikoutil.toImage
import matt.image.heavy.skikoutil.toSkiaColor
import matt.nn.deephys.model.importformat.im.DeephyImage
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType.PREMUL
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint

fun DeephyImage<*>.toSkiaImage(): Image {

    val mat = matrix

    val imageInfo =
        ImageInfo(
            width = mat[0].size,
            height = mat.size,
            colorInfo =
                ColorInfo(
                    ColorType.RGBA_8888,
                    PREMUL,
                    ColorSpace.sRGB
                )
        )

    val outputBitmap =
        Bitmap().apply {
            setImageInfo(imageInfo)
            allocPixelsAndCheck()
        }
    val canv = Canvas(outputBitmap)
    Canvas(outputBitmap).apply {
        mat.forEachIndexed { y, row ->
            row.forEachIndexed { x, pix ->
                canv.drawPoint(
                    x = x.toFloat(),
                    y = y.toFloat(),
                    paint =
                        Paint().apply {
                            color = pix.toMcolor().roundToRGBA().toSkiaColor()
                        }
                )
            }
        }
    }
    return outputBitmap.toImage()
}
