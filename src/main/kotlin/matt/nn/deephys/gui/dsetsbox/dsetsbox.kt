package matt.nn.deephys.gui.dsetsbox

import javafx.application.Platform.runLater
import matt.file.CborFile
import matt.file.toSFile
import matt.fx.control.toggle.mech.ToggleMechanism
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.model.data.message.FileList
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
import matt.obs.prop.BindableProperty

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


  val myToggleGroup = ToggleMechanism<DatasetViewer>()/*.apply {
	this.selectedValue.onChange {
	  isChangingBindingM.value = true
	  runLater {
		isChangingBindingM.value = false
	  }
	  *//*prevents infinite recursion stack overflows in some cases*//*
	  *//*must be the first listener for "myToggleGroup"... and I'm concerned this enforced enough*//*
	  *//*maybe not the best solution*//*
	  *//*or maybe is the best solution but needs to be generalized / canonicalize better*//*
	}
  }*/

  //  private val isChangingBindingM = BindableProperty(false)
  //  val isChangingBinding = isChangingBindingM.readOnly()
  private val boundM = BindableProperty<DatasetViewer?>(null)
  val bound = boundM.readOnly()

  init {
	myToggleGroup.selectedValue.onChange {
	  boundM.value =
		null /*necessary to remove all binding and reset everything before adding new binding or risk weird infinite recursions while changing binding and DatasetViewers are looking at each other infinitely looking for topNeurons*/
	  boundM.value = it
	}
  }


  //  val bound: Var<DatasetViewer?> = myToggleGroup.selectedValue

  fun addTest() = DatasetViewer(null, this).also { plusAssign(it) }


  fun removeTest(t: DatasetViewer) {
	println("removing test: ${t.file.value}")
	if (bound.value == t) myToggleGroup.selectedValue.value = null
	t.removeFromParent()
	t.normalizeTopNeuronActivations.unbind()
	t.numImagesPerNeuronInByImage.unbind()
	t.predictionSigFigs.unbind()
	t.topNeurons.removeAllDependencies()
	t.boundTopNeurons.removeAllDependencies()
	t.boundToDSet.removeAllDependencies()
	t.outerBox.save()
  }

  fun removeAllTests() {
	/*need the toList here since concurrent modification exception is NOT being thrown and actually causing bugs*/
	children.toList().forEach {
	  removeTest(it)
	}
  }

}

