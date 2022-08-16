package matt.nn.deephy.gui.deephyimview

import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.wrapper.node.onLeftClick
import matt.klib.todoOnce
import matt.nn.deephy.gui.DatasetViewer
import matt.nn.deephy.gui.dataset.DatasetNode
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephy.gui.draw.draw
import matt.nn.deephy.model.DeephyImage

@Suppress("UNUSED_PARAMETER")
class DeephyImView(im: DeephyImage, viewer: DatasetViewer, dsNode: DatasetNode): ScaledCanvas() {
  init {
	todoOnce("combine draw methods for V1 and deephy")
	draw(im)
	node.hoverProperty().onChange {
	  if (it) {
		drawBorder()
	  } else {
		draw(im)
	  }
	}
	onLeftClick {
	  if (viewer.bound.value) {
		viewer.outerBox.myToggleGroup.selectToggle(null)
	  }
	  viewer.imageSelection.value = im
	  dsNode.view.value = ByImage
	}
  }
}