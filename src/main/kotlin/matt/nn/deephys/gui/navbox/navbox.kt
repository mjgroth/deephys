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
import matt.fx.graphics.wrapper.pane.hSpacer
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.spacer
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.style.FXColor
import matt.http.commons.GH_ORG_NAME
import matt.http.s3.rawS3Url
import matt.http.url.MURL
import matt.lang.unsafeErr
import matt.lang.url.toURL
import matt.model.data.rect.IntSquareSize
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.dataset.dtab.DeephysTabPane
import matt.nn.deephys.gui.global.deephyActionText
import matt.nn.deephys.gui.global.deephyButton
import matt.rstruct.loader.systemResourceLoader
import java.awt.Desktop
import java.net.URI

class ZooExample(
    val name: String,
    val modelURL: MURL,
    val testURLs: List<MURL>
)

class NavBox(private val app: DeephysApp) : VBoxW() {

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

        border = Border(
            BorderStroke(FXColor(0.5, 0.5, 0.5, 0.1), BorderStrokeStyle.SOLID, null, BorderWidths(1.0, 1.0, 0.0, 0.0))
        )

        /*border = Border()*/    /*borderProperty.bind(DarkModeController.darkModeProp.binding {
	  val c = if (it) FXColor(0.1f,0.1f,0.1f,1.0) else FXColor(0.9f,0.9f,0.9f,1.0)
	  Border(BorderStroke(c, BorderStrokeStyle.SOLID, null, BorderWidths.DEFAULT))
	})*/



        +DeephysTabPane().apply {

            alignment = TOP_CENTER

            fun zooURL(path: String) = rawS3Url(
                bucket = "deephys-tutorial-deps",
                path = path,
                region = "us-east-2"
            )

            this@NavBox.showDemosTab = deephysLazyTab("Neuronal Activity Zoo") {


                VBoxW().apply {
                    spacer()
                    listOf(
                        ZooExample(
                            name = "CIFAR",
                            modelURL = zooURL(
                                path = "ActivityZoo/CIFAR10_Example/resnet18_cifar.model",
                            ),
                            testURLs = listOf(
                                zooURL("ActivityZoo/CIFAR10_Example/CIFAR10.test"),
                                zooURL("ActivityZoo/CIFAR10_Example/CIFARV2.test"),
                            )
                        ),
                        ZooExample(
                            name = "Colored MNIST",
                            modelURL = zooURL("ActivityZoo/Colored_MNIST_Example/colored_mnist.model"),
                            testURLs = listOf(
                                zooURL("ActivityZoo/Colored_MNIST_Example/Colored_MNIST.test"),
                                zooURL("ActivityZoo/Colored_MNIST_Example/Permuted_colored_MNIST.test"),
                                zooURL("ActivityZoo/Colored_MNIST_Example/Arbitrary_colored_MNIST.test"),
                                zooURL("ActivityZoo/Colored_MNIST_Example/Drifted_colored_MNIST.test"),
                            )
                        ),
                        ZooExample(
                            name = "ImageNet ResNet18",
                            modelURL = zooURL("ActivityZoo/ResNet18_ImageNet/resnet18_imagenet.model"),
                            testURLs = listOf(
                                zooURL("ActivityZoo/ResNet18_ImageNet/ImageNetV1.test"),
                                zooURL("ActivityZoo/ResNet18_ImageNet/ImageNetV2.test"),
                                zooURL("ActivityZoo/ResNet18_ImageNet/ImageNet_sketch.test"),
                                zooURL("ActivityZoo/ResNet18_ImageNet/ImageNet_style.test"),
                            )
                        ),
                        ZooExample(
                            name = "ImageNet Cvt",
                            modelURL = zooURL("ActivityZoo/Cvt13/cvt13_imagenet.model"),
                            testURLs = listOf(
                                zooURL("ActivityZoo/Cvt13/ImageNetV1_cvt13.test"),
                                zooURL("ActivityZoo/Cvt13/ImageNetV2_cvt13.test"),
                                zooURL("ActivityZoo/Cvt13/ImageNet_sketch_cvt13.test"),
                                zooURL("ActivityZoo/Cvt13/ImageNet_style_cvt13.test"),
                            )
                        )
                    ).forEach { demo ->
                        deephyButton(demo.name) {
                            setOnAction {
                                this@NavBox.app.openZooDemo(demo)
                            }
                        }
                    }
                }
            }.apply {
                runLater {
                    this.isSelected = true
                }
            }
            deephysLazyTab("Links") {
                VBoxW().apply {
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
                                backupImage = svgToFXImage(
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