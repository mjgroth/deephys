package matt.nn.deephy.gui.deephyimview

import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.wrapper.node.onLeftClick
import matt.log.todoOnce
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephy.gui.draw.draw
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.ResolvedDeephyImage

class DeephyImView(im: ResolvedDeephyImage, viewer: DatasetViewer): ScaledCanvas() {
  init {
	todoOnce("combine draw methods for V1 and deephy")
	draw(im)
	node.hoverProperty().onChange {
	  if (it) drawBorder()
	  else draw(im)
	}
	onLeftClick {
	  if (viewer.isBound) viewer.outerBox.myToggleGroup.selectToggle(null)
	  viewer.imageSelection.value = im
	  viewer.view.value = ByImage
	}
  }
}