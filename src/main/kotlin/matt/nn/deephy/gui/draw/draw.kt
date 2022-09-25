package matt.nn.deephy.gui.draw

import matt.hurricanefx.wrapper.canvas.Canv
import matt.nn.deephy.model.DeephyImage

fun Canv.draw(image: DeephyImage) {
  pixelWidth = image.matrix[0].size.toDouble()
  pixelHeight = image.matrix.size.toDouble()
  val pw = graphicsContext.pixelWriter
  image.matrix.forEachIndexed { y, row ->
	row.forEachIndexed { x, pix ->
	  /* val r = pix[0]
	   val g = pix[1]
	   val b = pix[2]*/
	  pw.setColor(        /*x, y, FXColor.rgb((r*255.0).roundToInt(), (g*255.0).roundToInt(), (b*255.0).roundToInt())*/
		x, y, pix /*FXColor.rgb(pix[0], pix[1], pix[2])*/
	  )
	}
  }
}