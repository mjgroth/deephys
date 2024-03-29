package matt.nn.deephys.gui.dsetsbox

import javafx.application.Platform.runLater
import javafx.scene.layout.Border
import javafx.util.Duration
import matt.caching.compcache.ComputeCacheContextImpl
import matt.file.common.toAbsLinuxFile
import matt.file.construct.mFile
import matt.file.model.file.types.Cbor
import matt.file.model.file.types.TypedFile
import matt.file.types.checkType
import matt.fx.control.toggle.mech.ToggleMechanism
import matt.fx.control.wrapper.control.ControlWrapper
import matt.fx.graphics.anim.animation.keyframe
import matt.fx.graphics.anim.animation.timeline
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.style.FXColor
import matt.lang.model.file.MacFileSystem
import matt.math.ranges.step
import matt.model.data.message.AbsLinuxFile
import matt.nn.deephys.gui.global.color.DeephysPalette.deephysSelectGradient
import matt.nn.deephys.gui.global.deephyToggleButton
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
import matt.obs.bind.MyBinding
import matt.obs.prop.writable.BindableProperty

class DSetViewsVBox(
    val model: Model,
    private val settings: DeephysSettingsController
) : VBoxWrapperImpl<DatasetViewer>(childClass = DatasetViewer::class) {

    companion object {
        const val BIND_BUTTON_NAME = "Lead"
        const val NORMALIZER_BUTTON_NAME = "Normalizer"
    }

    private val cacheContext = ComputeCacheContextImpl()

    init {
        runLater {
            println("created $this")
        }
    }

    var modelVisualizer: ModelVisualizer? = null

    operator fun plusAssign(file: TypedFile<Cbor, *>) {
        this += DatasetViewer(file, this, settings, cacheContext)
    }

    operator fun plusAssign(list: List<AbsLinuxFile>) {
        list.forEach {
            this += (mFile(it.path, MacFileSystem)).checkType(Cbor)
        }
    }

    fun save() {
        DeephyState.tests.value = children.mapNotNull { it.file.value?.toAbsLinuxFile() }
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
        BIND_BUTTON_NAME,
        group = bindToggleGroup,
        value = viewer
    ) {
        setupSelectionColor(deephysSelectGradient)
    }

    private val inDToggleGroup = ToggleMechanism<DatasetViewer>()
    val normalizer = inDToggleGroup.selectedValue.readOnly()

    fun createInDToggleButton(
        parent: NodeWrapper,
        viewer: DatasetViewer
    ) = parent.deephyToggleButton(
        NORMALIZER_BUTTON_NAME,
        group = inDToggleGroup,
        value = viewer
    ) {
        setupSelectionColor(deephysSelectGradient)


        /*setupSelectionColor(Color.rgb(255, 255, 0, 0.1))


                textProperty.bind(selectedProperty.binding {
              if (it) "InD" else "OOD"
            })


        font = DEEPHY_FONT_MONO*/
    }


    fun selectViewerToBind(
        viewer: DatasetViewer?,
        makeInDToo: Boolean = false
    ) {
        bindToggleGroup.selectedValue v viewer
        if (makeInDToo) {
            inDToggleGroup.selectedValue v viewer
        }
    }


    fun addTest() =
        DatasetViewer(null, this, settings, cacheContext).also {
            plusAssign(it)
        }


    fun removeTest(t: DatasetViewer) {
        println("removing test: ${t.file.value}")
        if (bound.value == t) bindToggleGroup.selectedValue.value = null
        if (normalizer.value == t) inDToggleGroup.selectedValue.value = null
        t.removeFromParent()
        /* t.normalizeTopNeuronActivations.unbind() */
        t.normalizer.unbind()
        t.outerBoundDSet.unbind()
        t.numViewers.unbind()
        t.smallImageScale.unbind()
        t.bigImageScale.unbind()
        t.numImagesPerNeuronInByImage.unbind()
        t.predictionSigFigs.unbind()
        t.showCacheBars.unbind()
        t.showTutorials.unbind()
        t.topNeurons.removeAllDependencies()
        t.boundTopNeurons.removeAllDependencies()
        t.boundToDSet.removeAllDependencies()
        t.outerBox.save()
        requestFocus() /*make this into scene.oldFocusOwner to remove possibility of that causing memory leak*/
    }

    fun removeAllTests() {
        /*need the toList here since concurrent modification exception is NOT being thrown and actually causing bugs*/
        children.toList().forEach {
            removeTest(it)
        }
    }

    fun flashBindButtons() {
        /*might have debug children*/
        @Suppress("UselessCallOnCollection")
        val buttons = children.filterIsInstance<DatasetViewer>().mapNotNull { it.bindButton }
        flashControls(buttons)
    }

    fun flashOODButtons() {
        /*might have debug children*/
        @Suppress("UselessCallOnCollection")
        val buttons = children.filterIsInstance<DatasetViewer>().mapNotNull { it.oodButton }
        flashControls(buttons)
    }

    fun flashControls(controls: Collection<ControlWrapper>) {
        val t =
            timeline {
                val theStep = 1000
                (0..2000 step theStep).forEach { millis ->

                    val range = (0.0..1.0 step 0.1)

                    val base1 = millis.toDouble()
                    range.forEach { valu ->
                        keyframe(Duration.millis(base1 + theStep * valu * 0.5)) {
                            setOnFinished {
                                val b = Border.stroke(FXColor.rgb(255, 255, 0, valu))
                                controls.forEach {
                                    it.border = b
                                }
                            }
                        }
                    }
                    val base2 = base1 + theStep * 0.5
                    range.forEach { tim ->
                        val valu = 1.0 - tim
                        keyframe(Duration.millis(base2 + theStep * tim * 0.5)) {
                            setOnFinished {
                                val b = Border.stroke(FXColor.rgb(255, 255, 0, valu))
                                controls.forEach {
                                    it.border = b
                                }
                            }
                        }
                    }
                }
            }
        t.setOnFinished {
            controls.forEach {
                it.border = null
            }
        }
    }

    val highlightedNeurons =
        MyBinding(children) {
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

