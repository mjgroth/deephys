package matt.nn.deephys.gui.dsetsbox

import matt.file.CborFile
import matt.file.toSFile
import matt.fx.control.tfx.control.ToggleMechanism
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.model.message.FileList
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
import matt.obs.prop.Var

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


  val myToggleGroup = ToggleMechanism<DatasetViewer>()
  val bound: Var<DatasetViewer?> = myToggleGroup.selectedValue

  fun addTest() = plusAssign(DatasetViewer(null, this))

}

