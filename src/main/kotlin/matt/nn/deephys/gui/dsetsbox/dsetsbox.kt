package matt.nn.deephys.gui.dsetsbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import matt.caching.compcache.ComputeCacheContextImpl
import matt.compose.controls.accordion.Accordion
import matt.compose.state.readonly.readOnly
import matt.compose.state.toggle.NewToggleMechanism
import matt.file.JioFile
import matt.file.common.toAbsLinuxFile
import matt.file.construct.toJioFile
import matt.lang.err.unsafeError
import matt.lang.err.unsafeReturningErr
import matt.model.k.log.Logger
import matt.nn.deephys.gui.modelvis.ModelVisualizerState
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.unsafemigration.ControlWrapper
import matt.nn.deephys.gui.unsafemigration.NodeWrapper
import matt.nn.deephys.gui.viewer.DatasetViewer1
import matt.nn.deephys.gui.viewer.DatasetViewer2
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.model.ResolvedNeuron
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.state.DeephyState
import matt.obs.bind.MyBinding
import matt.osi.serfile.AbsLinuxFile

const val BIND_BUTTON_NAME = "Lead"
const val NORMALIZER_BUTTON_NAME = "Normalizer"

class DSetViewsState(
    val deephyState: DeephyState,
    val modelVisualizer: ModelVisualizerState
) {

    @Suppress("unused")
    private val cacheContext = ComputeCacheContextImpl()

    private val bindToggleGroup = NewToggleMechanism<DatasetViewerState>(unsafeReturningErr())
    private val boundM =
        derivedStateOf {
            bindToggleGroup.selected.value
        }
    val bound = boundM.readOnly()

    private val inDToggleGroup = NewToggleMechanism<DatasetViewerState>(unsafeReturningErr())
    val normalizer = inDToggleGroup.selected.readOnly()

    val datasets = mutableStateListOf<DatasetViewerState>()

    operator fun plusAssign(cborFile: JioFile) {
        unsafeError(
            """
            this += DatasetViewer(file, this, settings, cacheContext)        
            """.trimIndent()
        )
    }
    operator fun plusAssign(list: List<AbsLinuxFile>) {
        list.forEach {
            this += it.toJioFile()
        }
    }

    fun save() {
        deephyState.tests.value = datasets.mapNotNull { it.file.value?.toAbsLinuxFile() }
    }

    @Suppress("UnusedParameter")
    fun createBindToggleButton(
        @Suppress("unused") parent: NodeWrapper,
        @Suppress("unused") viewer: DatasetViewerState
    ): Any =
        unsafeReturningErr(
            """
                     = parent.deephyToggleButton(
                BIND_BUTTON_NAME,
                group = bindToggleGroup,
                value = viewer
            ) {
                unsafeReturningErr(
                    ""${'"'}
                    setupSelectionColor(DeephysPalette.deephysSelectGradient)    
                    ""${'"'}.trimIndent()
                )
            }
            """.trimIndent()
        )

    @Suppress("UnusedParameter")
    fun createInDToggleButton(
        @Suppress("unused") parent: NodeWrapper,
        @Suppress("unused") viewer: DatasetViewerState
    ): Any =
        unsafeReturningErr(
            """
                parent.deephyToggleButton(
                NORMALIZER_BUTTON_NAME,
                group = inDToggleGroup,
                value = viewer
            ) {
                unsafeErr(
                    ""${'"'}
                    setupSelectionColor(DeephysPalette.deephysSelectGradient)    
                    ""${'"'}.trimIndent()
                )



                /*setupSelectionColor(Color.rgb(255, 255, 0, 0.1))


                    textProperty.bind(selectedProperty.binding {
                  if (it) "InD" else "OOD"
                })


            font = DEEPHY_FONT_MONO*/
            }
            """.trimIndent()
        )

    @Suppress("UnusedParameter")
    fun selectViewerToBind(
        @Suppress("unused") viewer: DatasetViewerState?,
        @Suppress("unused") makeInDToo: Boolean = false
    ) {
        unsafeError(
            """
            bindToggleGroup.selectedValue v viewer
            if (makeInDToo) {
                inDToggleGroup.selectedValue v viewer
            }    
            """.trimIndent()
        )
    }

    @Suppress("unused")
    fun addTest(): DatasetViewerState =
        unsafeReturningErr(
            """
            DatasetViewerState(null, this, settings, cacheContext).also {
                plusAssign(it)
            } 
            """.trimIndent()
        )

    fun removeTest(t: DatasetViewerState) {
        unsafeError(
            """
            println("removing test: ${t.file.value}")
            if (bound.value == t) bindToggleGroup.selectedValue.value = null
            if (normalizer.value == t) inDToggleGroup.selectedValue.value = null
            t.removeFromParent()
            /* t.normalizeTopNeuronActivations.unbind() */
            t.outerBox.save()
            requestFocus() /*make this into scene.oldFocusOwner to remove possibility of that causing memory leak*/    
            """.trimIndent()
        )
    }

    @Suppress("unused")
    fun removeAllTests() {
        /*need the toList here since concurrent modification exception is NOT being thrown and actually causing bugs*/
        datasets.toList().forEach {
            removeTest(it)
        }
    }

    fun flashBindButtons() {
        val buttons = datasets.mapNotNull { it.bindButton }
        flashControls(buttons)
    }

    fun flashOODButtons() {
        val buttons = datasets.mapNotNull { it.oodButton }
        flashControls(buttons)
    }

    @Suppress("UnusedParameter")
    private fun flashControls(
        @Suppress("unused") controls: Collection<ControlWrapper>
    ) {
        unsafeError(
            """
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
            """.trimIndent()
        )
    }

    @Suppress("unused")
    val highlightedNeurons: MyBinding<List<ResolvedNeuron>> =
        unsafeReturningErr(
            """
            MyBinding(children) {
                datasets.flatMap { it.highlightedNeurons.value }
            }.apply {
                datasets.onChange {
                    removeAllDependencies()
                    datasets.forEach {
                        addDependency(it.highlightedNeurons)
                    }
                    markInvalid()
                }
                datasets.forEach {
                    addDependency(it.highlightedNeurons)
                }
            }        
            """.trimIndent()
        )
}

@Suppress("UnusedParameter", "unused")
@Composable
context(_: Logger)
fun DSetViewsVBox(
    state: DSetViewsState,
    model: Model,
    settings: DeephysSettingsController
) {

    /*titleProperty.bind(file.binding { it?.nameWithoutExtension })*/

    Accordion(
        state.datasets,
        defaultExpanded = { true },
        titleContent = {
            DatasetViewer1(it, settings)
        }
    ) {
        DatasetViewer2(it, settings)
    }
}
