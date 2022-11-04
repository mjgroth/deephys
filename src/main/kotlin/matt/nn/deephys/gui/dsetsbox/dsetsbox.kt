package matt.nn.deephys.gui.dsetsbox

import javafx.application.Platform.runLater
import matt.file.CborFile
import matt.file.toSFile
import matt.fx.control.tfx.control.ToggleMechanism
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.model.message.FileList
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
import matt.obs.prop.BindableProperty
import matt.obs.prop.Var

class DSetViewsVBox(val model: Model): VBoxWrapperImpl<DatasetViewer>() {

  init {
	runLater {
	  println("created $this")
	}
  }

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


  val myToggleGroup = ToggleMechanism<DatasetViewer>().apply {
	this.selectedValue.onChange {
	  isChangingBindingM.value = true
	  runLater {
		isChangingBindingM.value = false
	  }
	  /*prevents infinite recursion stack overflows in some cases*/
	  /*must be the first listener for "myToggleGroup"... and I'm concerned this enforced enough*/
	  /*maybe not the best solution*/
	  /*or maybe is the best solution but needs to be generalized / canonicalize better*/
	}
  }
  private val isChangingBindingM = BindableProperty(false)
  val isChangingBinding = isChangingBindingM.readOnly()
  val bound: Var<DatasetViewer?> = myToggleGroup.selectedValue

  fun addTest() = DatasetViewer(null, this).also { plusAssign(it) }

}

