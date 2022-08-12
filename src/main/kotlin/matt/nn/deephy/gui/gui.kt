package matt.nn.deephy.gui

import javafx.scene.control.ContentDisplay.LEFT
import javafx.stage.DirectoryChooser
import matt.file.construct.toMFile
import matt.fx.graphics.FXColor
import matt.hurricanefx.eye.prop.stringBinding
import matt.hurricanefx.wrapper.canvas.CanvasWrapper
import matt.hurricanefx.wrapper.pane.hbox.HBoxWrapper
import matt.hurricanefx.wrapper.target.label
import matt.nn.deephy.model.DeephyDataManager
import matt.nn.deephy.model.DeephyImage
import kotlin.math.roundToInt

val dataFolderNode by lazy {
  HBoxWrapper().apply {
	label(DeephyDataManager.dataFolderProperty.stringBinding { "data folder: ${it?.tildeString()}" }) {
	  contentDisplay = LEFT
	  graphic = button("▼") {
		tooltip("choose data folder")
		setOnAction {
		  val f = DirectoryChooser().apply {
			title = "choose data folder"
		  }.showDialog(stage)

		  if (f != null) {
			DeephyDataManager.dataFolderProperty.value = f.toMFile()
		  }
		}
	  }
	}
  }
}

fun CanvasWrapper.draw(image: DeephyImage) {
  node.width = image.matrix[0].size.toDouble()
  node.height = image.matrix.size.toDouble()
  val pw = graphicsContext2D.pixelWriter
  image.matrix.forEachIndexed { y, row ->
	row.forEachIndexed { x, pix ->
	  val r = pix[0]
	  val g = pix[1]
	  val b = pix[2]
	  pw.setColor(
		x, y, FXColor.rgb((r*255.0).roundToInt(), (g*255.0).roundToInt(), (b*255.0).roundToInt())
	  )
	}
  }
}

@Deprecated(
  "this was for when pixel values were at weird values like -2.0 to 2.0 which Anirban confimed happens in some datasets (one of the CIFARs). It is buggy, obviously."
) fun CanvasWrapper.drawOld(image: DeephyImage) {
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