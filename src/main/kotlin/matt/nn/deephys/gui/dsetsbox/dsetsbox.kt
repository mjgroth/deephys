package matt.nn.deephys.gui.dsetsbox

import javafx.scene.control.ToggleGroup
import matt.file.CborFile
import matt.file.toSFile
import matt.fx.control.tfx.control.selectedValueProperty
import matt.hurricanefx.eye.wrapper.obs.obsval.prop.toNullableProp
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
import matt.obs.prop.Var
import matt.model.message.FileList

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

