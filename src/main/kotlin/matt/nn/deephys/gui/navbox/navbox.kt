package matt.nn.deephys.gui.navbox

import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.Priority.ALWAYS
import matt.fx.control.wrapper.button.toggle.ToggleButtonWrapper
import matt.fx.graphics.fxthread.runLater
import matt.fx.graphics.icon.fav.FaviconLoader
import matt.fx.graphics.icon.svg.svgToFXImage
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.pane.hSpacer
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.style.FXColor
import matt.lang.common.unsafeErr
import matt.lang.matt.GH_ORG_NAME
import matt.lang.url.toURL
import matt.model.data.rect.IntSquareSize
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.dataset.dtab.DeephysTabPane
import matt.nn.deephys.gui.global.deephyActionText
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.navbox.zoo.NeuronalActivityZoo
import matt.rstruct.loader.desktop.systemResourceLoader
import java.awt.Desktop
import java.net.URI


class NavBox(private val app: DeephysApp) : VBoxW(childClass = NodeWrapper::class) {

    var showDemosTab: ToggleButtonWrapper? = null

    fun showDemos() {
        showDemosTab!!.isSelected = true
    }

    init {

        alignment = TOP_CENTER


        minWidth = 300.0
        vgrow = ALWAYS
        isFillWidth = true
        spacing = 10.0

        border =
            Border(
                BorderStroke(FXColor(0.5, 0.5, 0.5, 0.1), BorderStrokeStyle.SOLID, null, BorderWidths(1.0, 1.0, 0.0, 0.0))
            )

        /*border = Border()

        borderProperty.bind(DarkModeController.darkModeProp.binding {
	  val c = if (it) FXColor(0.1f,0.1f,0.1f,1.0) else FXColor(0.9f,0.9f,0.9f,1.0)
	  Border(BorderStroke(c, BorderStrokeStyle.SOLID, null, BorderWidths.DEFAULT))
	})

         */



        +DeephysTabPane().apply {

            alignment = TOP_CENTER



            this@NavBox.showDemosTab =
                deephysLazyTab("Neuronal Activity Zoo") {


                    VBoxW(childClass=NodeWrapper::class).apply {
                        spacer()
                        NeuronalActivityZoo.EXAMPLES.forEach { demo ->
                            deephyButton(demo.name) {
                                setOnAction {
                                    this@NavBox.app.openZooDemo(demo)
                                }
                            }
                        }
                    }
                }.apply {
                    runLater {
                        isSelected = true
                    }
                }
            deephysLazyTab("Links") {
                VBoxW(childClass=NodeWrapper::class).apply {
                    spacer()
                    alignment = CENTER
                    mapOf(
                        "Homepage" to "https://deephys.org/",
                        "Neural Activity Zoo" to "https://deephys.org/",
                        "Documentation" to "https://deephys.readthedocs.io/en/latest/?badge=latest",
                        "GitHub" to "https://github.com/$$GH_ORG_NAME/deephys-aio",
                        "Report a Bug" to "https://deephys.youtrack.cloud/"
                    ).forEach { label, url ->
                        h {
                            hSpacer(50.0)
                            alignment = CENTER_LEFT
                            spacing = 7.5


                            val favSize = 18
                            unsafeErr("Is the backupImage below still the correct dimensions? I think I used to specify only the width OR the height. But since moving to a size-based approach I now specify both. I am just unsure if web.svg is in fact supposed to be shown with a square shape or if that is a distortion of its aspect ratio")
                            +FaviconLoader.loadAsynchronously(
                                url = url.toURL(),
                                backupImage =
                                    svgToFXImage(
                                        systemResourceLoader().resourceStream("web.svg")!!,
                                        IntSquareSize(favSize)
                                    ),
                                fitSize = IntSquareSize(favSize).toDoubleSize()
                            )

                            /*svgIcon("web.svg", favSize)*/


                            deephyActionText(label) {
                                Desktop.getDesktop().browse(URI(url))
                            }
                        }
                    }
                }
            }
        }
    }
}
