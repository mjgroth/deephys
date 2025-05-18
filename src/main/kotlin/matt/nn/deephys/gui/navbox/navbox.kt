@file:Suppress("unused")

package matt.nn.deephys.gui.navbox

import androidx.compose.runtime.Composable
import matt.lang.common.unsafeError
import matt.nn.deephys.gui.DeephysApp



@Suppress("UnusedParameter")
@Composable
fun NavBox(
    app: DeephysApp
) {
    unsafeError(
        $$"""
        Column(
            Modifier
                .widthIn(min = 300.dp)
                .border(
                    border =
                        BorderStroke(
                            color = FloatColor(0.5f, 0.5f, 0.5f, 0.1f).toComposeColor(),
                            width = unsafeReturningErr("BorderWidths(1.0, 1.0, 0.0, 0.0)")
                        )
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            borderProperty.bind(
                DarkModeController.darkModeProp.binding {
                    val c = if (it) FXColor(0.1f, 0.1f, 0.1f, 1.0) else FXColor(0.9f, 0.9f, 0.9f, 1.0)
                    Border(BorderStroke(c, BorderStrokeStyle.SOLID, null, BorderWidths.DEFAULT))
                }
            )




            DeephysTabPane {

                alignment = TOP_CENTER



                this@NavBox.showDemosTab =
                    Tab("Neuronal Activity Zoo") {


                        Column {
                            SpacerWithOldFxSize()
                            NeuronalActivityZoo.EXAMPLES.forEach { demo ->
                                DeephyButton(demo.name) {
                                    this@NavBox.app.openZooDemo(demo)
                                }
                            }
                        }
                    }.apply {
                        isSelected = true
                    }
                Tab("Links") {
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
                                Spacer(Modifier.width(50.0.dp))


                                val favSize = 18
                                unsafeErr(
                                    "Is the backupImage below still the correct dimensions? I think I used to specify only the width OR the height. But since moving to a size-based approach I now specify both. I am just unsure if web.svg is in fact supposed to be shown with a square shape or if that is a distortion of its aspect ratio"
                                )
                                val svgResourceStream = systemResourceLoader().resourceStream("web.svg")!!
                                val svgBytes = svgResourceStream.readAllBytes()
                                val svg = Svg(svgBytes.decodeToString())


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

                                /*svgIcon("web.svg", favSize)*/


                                DeephyActionText(label) {
                                    Desktop.getDesktop().browse(URI(url))
                                }
                            }
                        }
                    }
                }
            }
        }       
        """.trimIndent()
    )
}
