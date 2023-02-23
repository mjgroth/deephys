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
import matt.lang.url.toURL
import matt.mstruct.rstruct.resourceStream
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.dataset.dtab.DeephysTabPane
import matt.nn.deephys.gui.global.deephyActionText
import matt.nn.deephys.gui.global.deephyButton
import java.awt.Desktop
import java.net.URI

class ZooExample(
  val name: String,
  val modelURL: String,
  val testURLs: List<String>
)

class NavBox(private val app: DeephysApp): VBoxW() {

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

	  this@NavBox.showDemosTab = deephysLazyTab("Neuronal Activity Zoo") {


		VBoxW().apply {
		  spacer()
		  listOf(
			ZooExample(
			  name = "CIFAR Demo",
			  modelURL = "https://deephys.nyc3.digitaloceanspaces.com/zoo%2Fv1%2Fcifar%2Fresnet18_cifar.model",
			  testURLs = listOf(
				"https://deephys.nyc3.digitaloceanspaces.com/zoo%2Fv1%2Fcifar%2FCIFAR10.test",
				"https://deephys.nyc3.digitaloceanspaces.com/zoo%2Fv1%2Fcifar%2FCIFARV2.test"
			  )
			),
			ZooExample(
			  name = "Colored MNIST Demo",
			  modelURL = "https://deephys.nyc3.digitaloceanspaces.com/zoo%2Fv1%2Fcolored_mnist%2Fcolored_mnist.model",
			  testURLs = listOf(
					  "https://deephys.nyc3.digitaloceanspaces.com/zoo%2Fv1%2Fcolored_mnist%2FColored_MNIST.test",
					  "https://deephys.nyc3.digitaloceanspaces.com/zoo%2Fv1%2Fcolored_mnist%2FPermuted_colored_MNIST.test",
					"https://deephys.nyc3.digitaloceanspaces.com/zoo%2Fv1%2Fcolored_mnist%2FArbitrary_colored_MNIST.test",
					"https://deephys.nyc3.digitaloceanspaces.com/zoo%2Fv1%2Fcolored_mnist%2FNoisy_colored_MNIST.test"
			  )
			)
		  ).forEach { demo ->
			deephyButton("Open ${demo.name}") {
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
			"GitHub" to "https://github.com/mjgroth/deephys-aio",
			"Report a Bug" to "https://deephys.youtrack.cloud/"
		  ).forEach { label, url ->
			h {
			  hSpacer(50.0)
			  alignment = CENTER_LEFT
			  spacing = 7.5


			  val favSize = 18
			  +FaviconLoader.loadAsynchronously(
				url = url.toURL(),
				backupImage = svgToFXImage(resourceStream("web.svg")!!, favSize),
				fitWidth = favSize.toDouble(),
				fitHeight = favSize.toDouble()
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