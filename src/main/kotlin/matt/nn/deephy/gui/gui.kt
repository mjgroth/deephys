package matt.nn.deephy.gui

import javafx.scene.control.ToggleGroup
import matt.file.CborFile
import matt.file.toSFile
import matt.hurricanefx.tornadofx.control.selectedValueProperty
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.Model
import matt.nn.deephy.state.DeephyState
import matt.stream.message.FileList


class DSetViewsVBox(val model: Model): VBoxWrapper<DatasetViewer>() {
  operator fun plusAssign(file: CborFile) {
	this += DatasetViewer(file, this)
  }

  fun save() {
	DeephyState.tests.value = FileList(children.mapNotNull { it.fileProp.value?.toSFile() })
  }


  val myToggleGroup = ToggleGroup()

  val bound = myToggleGroup.selectedValueProperty<DatasetViewer>()

}

