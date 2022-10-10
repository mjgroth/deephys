package matt.nn.deephys.gui.deephyimview

import matt.fx.graphics.wrapper.node.onLeftClick
import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.hurricanefx.eye.lib.onChange
import matt.log.todoOnce
import matt.nn.deephys.gui.draw.draw
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.DeephyImage

class DeephyImView(im: DeephyImage, viewer: DatasetViewer): ScaledCanvas() {

  init {
	todoOnce("combine draw methods for V1 and deephy")
	draw(im)
	deephyTooltip(im.category.label, im)
	node.hoverProperty().onChange {
	  if (it) drawBorder()
	  else draw(im)
	}
	onLeftClick {
	  viewer.navigateTo(im)
	}
  }
}