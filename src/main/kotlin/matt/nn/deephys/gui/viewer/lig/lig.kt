package matt.nn.deephys.gui.viewer.lig

import javafx.scene.text.Font
import matt.rstruct.loader.desktop.systemResourceLoader


/*https://mail.openjdk.org/pipermail/openjfx-dev/2022-October/036313.html*/
object LigatureFont {
    /*val ligatureFont = "Fira Code"*/
    val ligatureFont: Font?  =
        Font.loadFont(
            systemResourceLoader().resourceURL("font/FiraCode-Bold.ttf").toString(),
            /*"file:font/FiraCode-Bold.ttf",*/
            45.0
        )
  /*init {

	javafx.scene.text.Font.loadFont("file:resources/font/FiraCode-Bold.ttf", 45.0)
	javafx.scene.text.Font.loadFont("file:resources/font/FiraCode-Regular.ttf", 45.0)


  }*/
}
