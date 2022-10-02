package matt.nn.deephy.gui.dsetsbox

import javafx.scene.control.ToggleGroup
import matt.file.CborFile
import matt.file.toSFile
import matt.hurricanefx.eye.wrapper.obs.obsval.prop.toNullableProp
import matt.hurricanefx.tornadofx.control.selectedValueProperty
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.nn.deephy.gui.modelvis.ModelVisualizer
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.model.importformat.Model
import matt.nn.deephy.state.DeephyState
import matt.obs.prop.Var
import matt.stream.message.FileList

class DSetViewsVBox(val model: Model): VBoxWrapperImpl<DatasetViewer>() {

  var modelVisualizer: ModelVisualizer? = null

  operator fun plusAssign(file: CborFile) {
	this += DatasetViewer(file, this)
  }

  operator fun plusAssign(list: FileList) {
	list.forEach {
	  this += (CborFile(it.path))
	}
  }

  fun save() {
	DeephyState.tests.value = FileList(children.mapNotNull { it.file.value?.toSFile() })
  }


  val myToggleGroup = ToggleGroup()
  val bound: Var<DatasetViewer?> = myToggleGroup.selectedValueProperty<DatasetViewer>().toNullableProp()

  fun addTest() = plusAssign(DatasetViewer(null, this))

}

