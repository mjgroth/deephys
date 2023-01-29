package matt.nn.deephys.gui.deephyimview

import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import matt.async.queue.pool.FakeWorkerPool
import matt.async.queue.pool.QueueWorkerPool
import matt.fx.control.menu.context.mcontextmenu
import matt.fx.graphics.fxthread.ensureInFXThreadOrRunLater
import matt.fx.graphics.wrapper.node.onLeftClick
import matt.fx.graphics.wrapper.style.toAwtColor
import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.lang.RUNTIME
import matt.log.todo.todoOnce
import matt.nn.deephys.gui.draw.draw
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.obs.math.double.op.div
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.awt.image.DataBufferInt
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.milliseconds

class DeephyImView(
  im: DeephyImage<*>,
  viewer: DatasetViewer,
  big: Boolean = false,
  loadAsync: Boolean = false
): ScaledCanvas(
  initializeInLoadingMode = true,
  progressIndicatorWidthAndHeight = (if (big) viewer.bigImageScale.value else viewer.smallImageScale.value),
  delayLoadingIndicatorBy = 1000.milliseconds
) {

  companion object {
	val realPool = QueueWorkerPool(RUNTIME.availableProcessors(), "DeephyImView Worker")
	val fakePool = FakeWorkerPool() /*because of the flickering*/
  }

  private val weakViewer = viewer.weakRef
  val weakIm = im.weak

  init {
	todoOnce("combine draw methods for V1 and deephy")

	val pool = if (loadAsync) realPool else fakePool
	pool.schedule {
	  val mat = im.matrix
	  draw(mat)
	  if (hoverProperty.value) {
		drawBorder()
	  }
	  hoverProperty.onChangeWithAlreadyWeak(weakIm) { deRefedIm, h ->
		if (h) drawBorder()
		else draw(deRefedIm)
	  }
	  onLeftClick {
		click()
	  }
	  mcontextmenu {
		"download image" does {
		  val pngFile = FileChooser().apply {
			title = "choose where to save png"
			this.extensionFilters.setAll(ExtensionFilter("png", "*.png"))
			this.initialFileName = im.category.label + "_" + im.index.toString() + ".png"
		  }.showSaveDialog(stage?.node)
		  if (pngFile != null) {

			val mat2 = weakIm.deref()!!.matrix
			val bi = BufferedImage(mat2.size, mat2[0].size, TYPE_INT_ARGB)
			val pixelData = (bi.raster.dataBuffer as DataBufferInt).data

			println("bi.width = ${bi.width}")
			println("bi.height = ${bi.height}")
			println("pixelData.length = ${pixelData.size}")

			var i = 0
			mat2.forEach {
			  it.forEach {
				val awt = it.toAwtColor()
				pixelData[i++] = ByteBuffer.wrap(
				  byteArrayOf(
					awt.alpha.toByte(),
					awt.red.toByte(),
					awt.green.toByte(),
					awt.blue.toByte()
				  )
				).asIntBuffer().get()
			  }
			}


			ImageIO.write(
			  bi,
			  "png",
			  pngFile
			)
		  }

		}
	  }
	  mat
	}.whenDone { mat ->
	  ensureInFXThreadOrRunLater {
		showCanvas()
		veryLazyDeephysTooltip(im.category.label, weakIm)
		val widthMaybe = mat[0].size.toDouble()
		if (big) {
		  scale.bindWeakly(viewer.bigImageScale/widthMaybe)
		} else {
		  scale.bindWeakly(viewer.smallImageScale/widthMaybe)
		}
	  }
	}


  }

  fun click() {
	weakViewer.deref()!!.navigateTo(weakIm.deref()!!)
  }

}