package matt.nn.deephys.gui.dsetsbox

import javafx.application.Platform.runLater
import javafx.scene.paint.Color
import matt.file.CborFile
import matt.file.toSFile
import matt.fx.control.toggle.mech.ToggleMechanism
import matt.fx.graphics.style.DarkModeController
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.model.data.message.FileList
import matt.nn.deephys.gui.global.DEEPHY_FONT_MONO
import matt.nn.deephys.gui.global.deephyToggleButton
import matt.nn.deephys.gui.global.deephysSelectColor
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephySettings
import matt.nn.deephys.state.DeephyState
import matt.obs.bind.MyBinding
import matt.obs.bind.binding
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


  private val bindToggleGroup = ToggleMechanism<DatasetViewer>()
  private val boundM = BindableProperty<DatasetViewer?>(null)
  val bound = boundM.readOnly()

  init {
	bindToggleGroup.selectedValue.onChange {
	  boundM.value =
		null /*necessary to remove all binding and reset everything before adding new binding or risk weird infinite recursions while changing binding and DatasetViewers are looking at each other infinitely looking for topNeurons*/
	  boundM.value = it
	}
  }

  fun createBindToggleButton(
	parent: NodeWrapper,
	viewer: DatasetViewer
  ) = parent.deephyToggleButton(
	"bind",
	group = bindToggleGroup,
	value = viewer
  ) {
	setupSelectionColor(deephysSelectColor)
  }

  private val inDToggleGroup = ToggleMechanism<DatasetViewer>()
  val inD = inDToggleGroup.selectedValue.readOnly()

  fun createInDToggleButton(
	parent: NodeWrapper,
	viewer: DatasetViewer
  ) = parent.deephyToggleButton(
	"",
	group = inDToggleGroup,
	value = viewer
  ) {
	setupSelectionColor(Color.rgb(255, 255, 0, 0.1))
	textProperty.bind(selectedProperty.binding {
	  if (it) "InD" else "OOD"
	})
	font = DEEPHY_FONT_MONO
  }


  fun selectViewerToBind(viewer: DatasetViewer?, makeInDToo: Boolean = false) {
	bindToggleGroup.selectedValue v viewer
	if (makeInDToo) {
	  inDToggleGroup.selectedValue v viewer
	}
  }


  fun addTest() = DatasetViewer(null, this).also { plusAssign(it) }


  fun removeTest(t: DatasetViewer) {
	println("removing test: ${t.file.value}")
	if (bound.value == t) bindToggleGroup.selectedValue.value = null
	if (inD.value == t) inDToggleGroup.selectedValue.value = null
	t.removeFromParent()
	t.normalizeTopNeuronActivations.unbind()
	t.inD.unbind()
	t.numImagesPerNeuronInByImage.unbind()
	t.predictionSigFigs.unbind()
	t.topNeurons.removeAllDependencies()
	t.boundTopNeurons.removeAllDependencies()
	t.boundToDSet.removeAllDependencies()
	t.outerBox.save()
	requestFocus() /*make this into scene.oldFocusOwner to remove possibility of that causing memory leak*/
	DeephySettings.millisecondsBeforeTooltipsVanish.cleanWeakListeners()
	DarkModeController.darkModeProp.cleanWeakListeners()
  }

  fun removeAllTests() {
	/*need the toList here since concurrent modification exception is NOT being thrown and actually causing bugs*/
	children.toList().forEach {
	  removeTest(it)
	}
  }

  val highlightedNeurons = MyBinding(children) {
	children.flatMap { it.highlightedNeurons.value }
  }.apply {
	children.onChange {
	  removeAllDependencies()
	  children.forEach {
		addDependency(it.highlightedNeurons)
	  }
	  markInvalid()
	}
	children.forEach {
	  addDependency(it.highlightedNeurons)
	}
  }
}

