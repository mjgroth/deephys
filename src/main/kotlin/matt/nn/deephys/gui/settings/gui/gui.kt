package matt.nn.deephys.gui.settings.gui

import javafx.scene.control.TreeItem
import javafx.scene.text.TextAlignment.CENTER
import matt.fx.control.lang.actionbutton
import matt.fx.control.wrapper.control.tree.treeview
import matt.fx.control.wrapper.treeitem.TreeItemWrapper
import matt.fx.graphics.wrapper.imageview.ImageViewWrapper
import matt.fx.graphics.wrapper.node.NW
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.stack.stackpane
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.gui.interact.openInNewWindow
import matt.gui.mstage.ShowMode.DO_NOT_SHOW
import matt.gui.mstage.WMode.CLOSE
import matt.gui.option.EnumSetting
import matt.gui.option.SettingsData
import matt.nn.deephys.gui.global.deephyRadioButton
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.node.DeephysNode
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.settings.gui.control.createControlFor
import matt.nn.deephys.init.gearImage

private object MONITOR

private var gotSettingsButton = false
fun settingsButton(settings: DeephysSettingsController) = lazy {
  synchronized(MONITOR) {
	require(!gotSettingsButton)
	gotSettingsButton = true
  }

  actionbutton(graphic = ImageViewWrapper(gearImage.await()).apply {
	isPreserveRatio = true
	fitWidth = 25.0
  }) {
	settingsWindow(settings).value.show()
  }
}





private var gotSettingsWindow = false
fun settingsWindow(settings: DeephysSettingsController) = lazy {
  synchronized(MONITOR) {
	require(!gotSettingsWindow)
	gotSettingsWindow = true
  }
  SettingsPane(settings).openInNewWindow(
	DO_NOT_SHOW,
	CLOSE,
	EscClosable = true,
	decorated = true,
	title = "Deephys Options",
  ).apply {
	width = 1000.0
  }
}




fun <E: Enum<E>> EnumSetting<E>.createRadioButtons(rec: NodeWrapper) = rec.apply {
  val tm = createBoundToggleMechanism()
  cls.java.enumConstants.forEach {
	deephyRadioButton((it as Enum<*>).name, tm, it) {
	  isSelected = prop.value == it
	}
  }
}

class SettingsPane(override val settings: DeephysSettingsController): VBoxWrapperImpl<NodeWrapper>(), DeephysNode {
  companion object {
	private var instance: SettingsPane? = null
  }

  init {
	synchronized(SettingsPane::class) {
	  require(instance == null)
	  instance = this
	}
  }

  init {

	val memSafeSettings = settings

	h {
	  val tv = treeview<SettingsData> {
		root = TreeItemWrapper(memSafeSettings)
		populate {
		  it.value.sections.map { it as SettingsData }
		}
		root!!.expandAll()
		select(root!!.node)
	  }
	  v {
		fun update(selection: TreeItem<SettingsData>?) {
		  clear()
		  selection?.value?.settings?.forEach { sett ->
			+createControlFor(sett, memSafeSettings)
		  } ?: run {
			stackpane<NW> {
			  prefHeightProperty.bindWeakly(this@v.heightProperty)
			  prefWidthProperty.bindWeakly(this@v.widthProperty)
			  deephysText("Select a section in the tree to edit its settings.") {
				textAlignment = CENTER
			  }
			}
		  }
		}
		tv.selectedItemProperty.onChange {
		  update(it)
		}
		update(tv.selectedItem)
	  }
	}
  }
}