package matt.nn.deephys.gui.deephyimview

import matt.fx.graphics.wrapper.node.onLeftClick
import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.lang.weak.WeakRef
import matt.log.todo.todoOnce
import matt.nn.deephys.gui.draw.draw
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.obs.math.double.op.div

class DeephyImView(
  im: DeephyImage<*>,
  viewer: DatasetViewer,
  big: Boolean = false
): ScaledCanvas() {

  val weakViewer = WeakRef(viewer)
  val weakIm = WeakRef(im)

  init {
	todoOnce("combine draw methods for V1 and deephy")
	draw(im)

	deephyTooltip(im.category.label, im)



	hoverProperty.onChangeWithAlreadyWeak(weakIm) { derefedIm, h ->
	  if (h) drawBorder()
	  else draw(derefedIm)
	}

	onLeftClick {
	  click()
	}

	if (big) {
	  scale.bindWeakly(viewer.bigImageScale / im.widthMaybe)
	} else {
	  scale.bindWeakly(viewer.smallImageScale / im.widthMaybe)
	}


  }

  fun click() {
	weakViewer.deref()!!.navigateTo(weakIm.deref()!!)
  }

}