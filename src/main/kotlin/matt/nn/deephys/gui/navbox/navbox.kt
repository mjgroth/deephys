package matt.nn.deephys.gui.navbox

import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.Priority.ALWAYS
import matt.fx.graphics.icon.fav.FaviconLoader
import matt.fx.graphics.icon.svg.svgToFXImage
import matt.fx.graphics.wrapper.pane.hSpacer
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.style.FXColor
import matt.lang.url.toURL
import matt.mstruct.rstruct.resourceStream
import matt.nn.deephys.gui.global.deephyActionText
import java.awt.Desktop
import java.net.URI

class NavBox: VBoxW() {
  init {

	alignment = CENTER

	minWidth = 300.0
	vgrow = ALWAYS
	isFillWidth = true
	spacing = 10.0
	border = Border(
	  BorderStroke(FXColor(0.5, 0.5, 0.5, 0.1), BorderStrokeStyle.SOLID, null, BorderWidths(1.0, 1.0, 0.0, 0.0))
	)    /*border = Border()*/    /*borderProperty.bind(DarkModeController.darkModeProp.binding {
	  val c = if (it) FXColor(0.1f,0.1f,0.1f,1.0) else FXColor(0.9f,0.9f,0.9f,1.0)
	  Border(BorderStroke(c, BorderStrokeStyle.SOLID, null, BorderWidths.DEFAULT))
	})*/



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