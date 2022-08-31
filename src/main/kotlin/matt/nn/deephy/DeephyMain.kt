package matt.nn.deephy

import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay.RIGHT
import javafx.scene.image.Image
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import matt.auto.myPid
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.file.CborFile
import matt.file.MFile
import matt.file.construct.toMFile
import matt.file.toMFile
import matt.file.toSFile
import matt.fx.graphics.lang.actionbutton
import matt.fx.graphics.win.interact.openInNewWindow
import matt.fx.graphics.win.stage.ShowMode.SHOW_AND_WAIT
import matt.fx.graphics.win.stage.WMode.CLOSE
import matt.gui.app.GuiApp
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.tornadofx.item.spinner
import matt.hurricanefx.wrapper.imageview.ImageViewWrapper
import matt.hurricanefx.wrapper.node.NodeWrapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.klib.lang.resourceStream
import matt.klib.str.taball
import matt.klib.weak.MemReport
import matt.nn.deephy.gui.DSetViewsVBox
import matt.nn.deephy.gui.viewer.DatasetViewer
import matt.nn.deephy.state.DeephyState
import matt.nn.deephy.version.VersionChecker

fun main(): Unit = GuiApp(decorated = true) {

  println("my pid = $myPid")
  println(MemReport())


  stage.title = "$appName $myVersion"
  stage.node.minWidth = 600.0
  stage.node.minHeight = 850.0
  stage.width = 600.0
  stage.height = 850.0

  VersionChecker.checkForUpdatesInBackground()

  root<VBoxWrapper<NodeWrapper>> {

	alignment = Pos.TOP_CENTER

	hbox<NodeWrapper> {
	  actionbutton("choose model file") {
		val f = FileChooser().apply {
		  extensionFilters.setAll(ExtensionFilter("model files", "*.model"))
		}.showOpenDialog(stage)?.toMFile()?.toSFile()
		if (f != null) {
		  DeephyState.tests.value = null
		  DeephyState.model.value = f
		}
	  }

	  actionbutton(graphic = ImageViewWrapper(Image(resourceStream("gear.png"))).apply {
		isPreserveRatio = true
		fitWidth = 25.0
	  }.node) {

		VBoxWrapper<NodeWrapper>().apply {
		  label {
			text = "Number of images per neuron in image view"
			contentDisplay = RIGHT
			graphic = spinner(min = 9, max = 18, initialValue = DeephyState.numImagesPerNeuronInByImage.value) {
			  prefWidth = 55.0
			  valueProperty().onChange {
				require(it != null)
				DeephyState.numImagesPerNeuronInByImage.value = it
			  }
			}
		  }

		}.openInNewWindow(
		  SHOW_AND_WAIT, CLOSE, EscClosable = true, decorated = true, title = "Deephy Options"
		)
	  }

	}

	swapper(DeephyState.model) {
	  VBoxWrapper<NodeWrapper>().apply {
		val multiAcc = DSetViewsVBox(this@swapper.toMFile().loadCbor()).apply {
		  DeephyState.tests.value?.forEach {
			this += (CborFile(it.path))
		  }
		}
		+multiAcc
		actionbutton("add dataset") {
		  multiAcc += DatasetViewer(null, multiAcc)
		  taball("children of multAcc:", multiAcc.node.children)
		}
	  }
	}

	vbox<NodeWrapper> {
	  vgrow = ALWAYS
	}

	vbox<NodeWrapper> {
	  alignment = Pos.BOTTOM_LEFT
	  +VersionChecker.statusNode
	}
  }

}.start()

inline fun <reified T: Any> MFile.loadCbor(): T = Cbor.decodeFromByteArray(readBytes())
