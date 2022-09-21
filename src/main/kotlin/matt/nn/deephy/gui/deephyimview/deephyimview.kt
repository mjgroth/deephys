package matt.nn.deephy.gui.deephyimview

import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.wrapper.node.onLeftClick
import matt.log.profile.tic
import matt.log.todoOnce
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephy.gui.draw.draw
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.DeephyImage

class DeephyImView(im: DeephyImage, viewer: DatasetViewer): ScaledCanvas() {
  init {
	todoOnce("combine draw methods for V1 and deephy")
	draw(im)
	node.hoverProperty().onChange {
	  if (it) drawBorder()
	  else draw(im)
	}
	onLeftClick {
	  val t = tic("left click image")
	  t.toc("start")
	  if (viewer.isBoundToDSet.value) viewer.outerBox.myToggleGroup.selectToggle(null)
	  t.toc("maybe set bind to null")
	  viewer.imageSelection.value = im
	  t.toc("set imageSelection")
	  viewer.view.value = ByImage
	  t.toc("set view")
	}
  }
}