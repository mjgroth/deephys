package matt.nn.deephys.gui.deephyimview

import matt.fx.graphics.wrapper.node.onLeftClick
import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.lang.weak.WeakRef
import matt.log.todo.todoOnce
import matt.nn.deephys.gui.draw.draw
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.DeephyImage

class DeephyImView(im: DeephyImage, viewer: DatasetViewer): ScaledCanvas() {

  init {
	todoOnce("combine draw methods for V1 and deephy")
	draw(im)

	deephyTooltip(im.category.label, im)


	val weakIm = WeakRef(im)
	hoverProperty.onChangeWithAlreadyWeak(weakIm) { derefedIm, h ->
	  if (h) drawBorder()
	  else draw(derefedIm)
	}
	val weakViewer = WeakRef(viewer)
	onLeftClick {
	  weakViewer.deref()!!.navigateTo(weakIm.deref()!!)
	}
  }
}