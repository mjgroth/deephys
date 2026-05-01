@file:Suppress("UnusedVariable", "UNUSED_VARIABLE")

package matt.nn.deephys.gui.navbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import matt.compose.controls.popups.MyDialog
import matt.compose.graphics.layout.AlignedRow
import matt.compose.graphics.padding.WidthSpacer
import matt.image.common.Svg
import matt.lang.err.unsafeError
import matt.lang.matt.GH_ORG_NAME
import matt.model.context.SuspendingAutomationService
import matt.model.j.browse
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.DeephysApp.ZooDownloaderState
import matt.nn.deephys.gui.dataset.dtab.DeephysEnumTabPane
import matt.nn.deephys.gui.dsetsbox.DSetViewsState
import matt.nn.deephys.gui.global.AsyncDeephyButton
import matt.nn.deephys.gui.global.DeephyActionText
import matt.nn.deephys.gui.global.DeephysLabel
import matt.nn.deephys.gui.global.SpacerWithOldFxSize
import matt.nn.deephys.gui.navbox.zoo.NeuronalActivityZoo
import matt.nn.deephys.state.DeephyState
import matt.nn.deephys.state.load
import matt.rstruct.loader.desktop.systemResourceLoader
import java.net.URI

enum class NavBoxTab {
    NeuronalActivityZoo,
    Links
}

@Suppress("UnusedParameter", "unused")
@Composable
context(suspendingAutomationContext: SuspendingAutomationService)
fun NavBox(
    app: DeephysApp,
    dSetViewsState: DSetViewsState,
    deephyState: DeephyState
) {
    val scope = rememberCoroutineScope()
    val borderColor = MaterialTheme.colorScheme.surfaceContainerLowest
    Column(
        Modifier
            .widthIn(min = 300.dp)
            .drawBehind {
                drawPath(
                    Path()
                        .apply {
                            moveTo(size.width, 0f)
                            lineTo(0f, 0f)
                            lineTo(0f, size.height)
                        },
                    borderColor,
                    style =
                        Stroke(
                            width = 1f
                        )
                )
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DeephysEnumTabPane(
            selected = app.selectedNavTab,
            labels = {
                when (it) {
                    NavBoxTab.NeuronalActivityZoo -> "Neuronal Activity Zoo"
                    NavBoxTab.Links               -> "Links"
                }
            }
        ) {
            when (it) {
                NavBoxTab.NeuronalActivityZoo ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SpacerWithOldFxSize()
                        NeuronalActivityZoo.EXAMPLES.forEach { demo ->
                            val state =
                                remember {
                                    mutableStateOf<ZooDownloaderState?>(null)
                                }
                            AsyncDeephyButton(s = demo.name) {
                                coroutineScope {
                                    val theState = ZooDownloaderState(demo, this)
                                    state.value = theState
                                    app.openZooDemo(theState)
                                    check(
                                        theState.done.value == theState.total
                                    )
                                    app.showNavBox.value = false
                                    load(
                                        modelFile = theState.modelFile.await(),
                                        testFiles = theState.testFiles.awaitAll(),
                                        dsetViewsState = dSetViewsState,
                                        deephyState = deephyState
                                    )
                                    state.value = null
                                }
                            }
                            val stateValue = state.value
                            if (stateValue != null) {
                                MyDialog(
                                    onDismissRequest = {}
                                ) {
                                    Column {

                                        DeephysLabel("Downloading ${demo.name}...")

                                        val prog =
                                            LinearProgressIndicator(
                                                progress = {
                                                    stateValue.progress.value.toFloat()
                                                }
                                            )

                                        DeephysLabel(
                                            "Loading Files... (${stateValue.done.value}/${stateValue.total})"
                                        )
                                    }
                                }
                            }
                        }
                    }

                NavBoxTab.Links               ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SpacerWithOldFxSize()
                        mapOf(
                            "Homepage" to "https://deephys.org/",
                            "Neural Activity Zoo" to "https://deephys.org/",
                            "Documentation" to "https://deephys.readthedocs.io/en/latest/?badge=latest",
                            "GitHub" to "https://github.com/$GH_ORG_NAME/deephys-aio",
                            "Report a Bug" to "https://deephys.youtrack.cloud/"
                        ).forEach { (label, url) ->
                            AlignedRow(horizontalArrangement = Arrangement.spacedBy(7.5.dp)) {
                                WidthSpacer(50.0.dp)

                                val favSize = 18
                                unsafeError(
                                    "Is the backupImage below still the correct dimensions? I think I used to specify only the width OR the height. But since moving to a size-based approach I now specify both. I am just unsure if web.svg is in fact supposed to be shown with a square shape or if that is a distortion of its aspect ratio"
                                )
                                val svgResourceStream = systemResourceLoader().resourceStream("web.svg")!!
                                val svgBytes = svgResourceStream.readAllBytes()
                                val svg = Svg(svgBytes.decodeToString())

                                unsafeError(
                                    """
                            +FaviconLoader.loadAsynchronously(
                                url = url.toURL(),
                                backupImage =
                                    svg.renderToSkiaImage(
                                        GenericRectSize(
                                            width = PhysicalPixel(favSize),
                                            height = PhysicalPixel(favSize)
                                        )
                                    ),
                                fitSize = IntSquareSize(favSize).toDoubleSize()
                            )
       
                                    """.trimIndent()
                                )

                                /*svgIcon("web.svg", favSize)*/

                                DeephyActionText(label) {
                                    scope.launch { suspendingAutomationContext.browse(URI(url)) }
                                }
                            }
                        }
                    }
            }
        }
    }
}
