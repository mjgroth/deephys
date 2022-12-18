package matt.nn.deephys.gui.draw

import matt.fx.graphics.wrapper.canvas.Canv
import matt.nn.deephys.model.importformat.im.DeephyImage

fun Canv.draw(image: DeephyImage) {
  val mat = image.matrix
  pixelWidth = mat.size.toDouble()
  pixelHeight = mat.size.toDouble()
  val pw = graphicsContext.pixelWriter
  mat.forEachIndexed { y, row ->
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