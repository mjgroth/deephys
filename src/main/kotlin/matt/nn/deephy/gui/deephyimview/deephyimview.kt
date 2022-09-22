package matt.nn.deephy.gui.deephyimview

import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.wrapper.node.onLeftClick
import matt.log.todoOnce
import matt.nn.deephy.gui.dataset.DatasetNodeView.ByImage
import matt.nn.deephy.gui.draw.draw
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.gui.viewer.SelectImage
import matt.nn.deephy.model.DeephyImage

class DeephyImView(im: DeephyImage, viewer: DatasetViewer): ScaledCanvas() {
  init {
//	val t = tic("create imView")
//	t.toc(1)
	todoOnce("combine draw methods for V1 and deephy")
//	t.toc(2)
	draw(im)
//	t.toc(3)
	node.hoverProperty().onChange {
	  if (it) drawBorder()
	  else draw(im)
	}
//	t.toc(4)
	onLeftClick {
//	  val leftClickStopwatch = tic("left click image")
//	  leftClickStopwatch.toc("start")
	  if (viewer.isBoundToDSet.value) viewer.outerBox.myToggleGroup.selectToggle(null)
//	  leftClickStopwatch.toc("maybe set bind to null")
	  viewer.imageSelection.value = im
	  //	  if (viewer.history.isNotEmpty()) {
	  for (i in (viewer.historyIndex.value + 1) until viewer.history.size) {
		viewer.history.removeAt(viewer.historyIndex.value + 1)
	  }
	  //		viewer.history.setAll(viewer.history.subList(0, viewer.historyIndex.value + 1))
	  //	  }
	  viewer.history.add(SelectImage(im))
	  viewer.historyIndex.value += 1

//	  leftClickStopwatch.toc("set imageSelection")
	  viewer.view.value = ByImage
//	  leftClickStopwatch.toc("set view")
	}
//	t.toc(5)
  }
}