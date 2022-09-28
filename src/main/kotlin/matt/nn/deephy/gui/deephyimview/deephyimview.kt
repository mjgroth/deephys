package matt.nn.deephy.gui.deephyimview

import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.wrapper.node.onLeftClick
import matt.log.todoOnce
import matt.nn.deephy.gui.draw.draw
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.importformat.DeephyImage

class DeephyImView(im: DeephyImage, viewer: DatasetViewer): ScaledCanvas() {
  init {
	todoOnce("combine draw methods for V1 and deephy")
	draw(im)
	node.hoverProperty().onChange {
	  if (it) drawBorder()
	  else draw(im)
	}
	onLeftClick {
	  viewer.navigateTo(im)
	}
  }
}