package matt.nn.deephy.gui

import javafx.scene.control.ToggleGroup
import javafx.scene.text.Font
import matt.file.CborFile
import matt.file.toSFile
import matt.hurricanefx.eye.wrapper.obs.obsval.prop.toNullableProp
import matt.hurricanefx.tornadofx.control.selectedValueProperty
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.Model
import matt.nn.deephy.state.DeephyState
import matt.obs.prop.Var
import matt.stream.message.FileList

val DEEPHY_FONT: Font = Font.font("Georgia")

class DSetViewsVBox(val model: Model): VBoxWrapper<DatasetViewer>() {
  operator fun plusAssign(file: CborFile) {
	this += DatasetViewer(file, this)
  }

  fun save() {
	DeephyState.tests.value = FileList(children.mapNotNull { it.file.value?.toSFile() })
  }


  val myToggleGroup = ToggleGroup()
  val bound: Var<DatasetViewer?> = myToggleGroup.selectedValueProperty<DatasetViewer>().toNullableProp()

}

