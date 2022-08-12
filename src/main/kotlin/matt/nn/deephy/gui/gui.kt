package matt.nn.deephy.gui

import matt.fx.graphics.FXColor
import matt.hurricanefx.wrapper.canvas.CanvasWrapper
import matt.nn.deephy.model.DeephyImage
import kotlin.math.roundToInt

fun CanvasWrapper.draw(image: DeephyImage) {
  node.width = image.matrix[0].size.toDouble()
  node.height = image.matrix.size.toDouble()
  val pw = graphicsContext2D.pixelWriter
  image.matrix.forEachIndexed { y, row ->
	row.forEachIndexed { x, pix ->
	  val r = maxOf(minOf((pix[0] + 1)/2, 1.0), 0.0)
	  val g = maxOf(minOf((pix[1] + 1)/2, 1.0), 0.0)
	  val b = maxOf(minOf((pix[2] + 1)/2, 1.0), 0.0)
	  pw.setColor(
		x, y, FXColor.rgb((r*255.0).roundToInt(), (g*255.0).roundToInt(), (b*255.0).roundToInt())
	  )
	}
  }

}