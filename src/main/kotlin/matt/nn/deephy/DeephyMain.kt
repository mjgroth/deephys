@file:OptIn(ExperimentalSerializationApi::class)

package matt.nn.deephy

import javafx.geometry.Pos
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.DirectoryChooser
import kotlinx.serialization.ExperimentalSerializationApi
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.file.construct.toMFile
import matt.fx.graphics.lang.actionbutton
import matt.gui.app.GuiApp
import matt.hurricanefx.eye.bind.toStringConverter
import matt.hurricanefx.eye.lang.SProp
import matt.hurricanefx.eye.prop.stringBinding
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.control.choice.ChoiceBoxWrapper
import matt.hurricanefx.wrapper.node.enableWhen
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.target.label
import matt.nn.deephy.gui.draw
import matt.nn.deephy.model.DeephyDataManager
import matt.nn.deephy.model.DeephyDataManager.cifarV1Test
import matt.nn.deephy.model.DeephyDataManager.dataFolderProperty
import matt.nn.deephy.model.Neuron
import matt.nn.deephy.version.VersionChecker

fun main(): Unit = GuiApp(decorated = true) {

  stage.title = "$appName $myVersion"
  stage.node.minWidth = 600.0
  stage.node.minHeight = 600.0
  stage.width = 600.0
  stage.height = 600.0


  val statusProp = SProp("")

  VersionChecker.checkForUpdatesInBackground()

  val resultBox = VBoxWrapper()

  root<VBoxWrapper> {
	label(dataFolderProperty.stringBinding { "data folder: $it" })

	hbox {
	  actionbutton("choose data folder") {
		val f = DirectoryChooser().apply {
		  title = "choose data folder"
		}.showDialog(stage)

		if (f != null) {
		  dataFolderProperty.value = f.toMFile()
		}
	  }
	  button("load data") {
		enableWhen { dataFolderProperty.isNotNull }
		setOnAction {

		  statusProp.value = if (dataFolderProperty.value == null) "please select data folder"
		  else if (cifarV1Test.value!!.doesNotExist) "${cifarV1Test.value} does not exist"
		  else {
			//			val (top, image) = DeephyDataManager.load()
			//			val (top2, image2) = DeephyDataManager.load2()

			val newData = DeephyDataManager.load3()

			//			println("image.category.size=${image.category.size}")

			//			val images = (0 until image.category.size).associate {
			//			  image.file_ID[it] to GoodImage(image, it)
			//			}
			//
			//			val images2 = (0 until image2.category.size).associate {
			//			  image2.file_ID[it] to GoodImage(image2, it)
			//			}

			//			val neurons = (0 until top.numNeurons).map {
			//			  Neuron(
			//				index = it,
			//				top100 = top.top100[it].map { images[it]!! }
			//			  )
			//			}.toObservable()
			//			val neurons2 = (0 until top2.numNeurons).map {
			//			  Neuron(
			//				index = it,
			//				top100 = top2.top100[it].map { images2[it]!! }
			//			  )
			//			}.toObservable()

			val theLayer = newData.layers[0]

			resultBox.clear()
			resultBox.apply {
			  label("Layer ID: ${theLayer.layerID}")
			  //			  label("Layer Name: ${top.layerName}")
			  label("Num Neurons: ${theLayer.neurons.size}")
			  var cb: ChoiceBoxWrapper<IndexedValue<Neuron>>? = null
			  hbox {
				label("choose neuron: ")
				cb = choicebox(values = theLayer.neurons.withIndex().toList()) {
				  converter = toStringConverter { "neuron ${it?.index}" }
				}
			  }
			  swapper(cb!!.valueProperty) {

				VBoxWrapper().apply {
				  text("dataset 1")
				  flowpane {
					(0 until 100).forEach { imIndex ->
					  val im = newData.images[value.activationIndexesHighToLow[imIndex]]

					  //					  val im = top100[imIndex]
					  canvas() {
						draw(im)
					  }
					}
					vgrow = ALWAYS
				  }
				  /*  text("dataset 2")
					flowpane {
					  (0 until 100).forEach { imIndex ->
						val im = neurons2[index].top100[imIndex]
						canvas() {
						  draw(im)
						}
					  }
					  vgrow = ALWAYS
					}*/
				}

			  }
			}
			"got data"
		  }
		}
	  }
	}

	+resultBox.apply {
	  vgrow = ALWAYS
	}
	vbox {
	  alignment = Pos.BOTTOM_LEFT
	  label(statusProp)
	  +VersionChecker.statusNode
	}
  }

}.start()


