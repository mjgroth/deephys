@file:OptIn(ExperimentalSerializationApi::class)

package matt.nn.deephy

import javafx.geometry.Pos
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.DirectoryChooser
import kotlinx.serialization.ExperimentalSerializationApi
import matt.exec.app.appName
import matt.exec.app.myVersion
import matt.file.construct.toMFile
import matt.fx.graphics.FXColor
import matt.fx.graphics.lang.actionbutton
import matt.gui.app.GuiApp
import matt.hurricanefx.eye.collect.toObservable
import matt.hurricanefx.eye.lang.SProp
import matt.hurricanefx.eye.prop.stringBinding
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.control.choice.ChoiceBoxWrapper
import matt.hurricanefx.wrapper.node.enableWhen
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.target.label
import matt.nn.deephy.model.DeephyDataManager
import matt.nn.deephy.model.DeephyDataManager.dataFile
import matt.nn.deephy.model.DeephyDataManager.dataFileTop
import matt.nn.deephy.model.DeephyDataManager.dataFolderProperty
import matt.nn.deephy.model.GoodImage
import matt.nn.deephy.model.Neuron
import matt.nn.deephy.version.VersionChecker
import kotlin.math.roundToInt

fun main(): Unit = GuiApp(decorated = true) {

  stage.title = "$appName $myVersion"

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
		  statusProp.value = if (dataFileTop.value == null) "please select data folder"
		  else if (dataFileTop.value!!.doesNotExist) "${dataFileTop.value} does not exist"
		  else if (dataFile.value!!.doesNotExist) "${dataFile.value} does not exist"
		  else {
			val (top, image) = DeephyDataManager.load()
			println("image.category.size=${image.category.size}")

			val images = (0 until image.category.size).associate {
			  image.file_ID[it] to GoodImage(
				fileID = image.file_ID[it],
				fileType = image.file_type[it],
				category = image.category[it],
				matrix = image.file[it].let {
				  val newRows = mutableListOf<MutableList<MutableList<Double>>>()

				  it.mapIndexed { colorIndex, singleColorMatrix ->
					singleColorMatrix.mapIndexed { rowIndex, row ->
					  val newRow =
						if (colorIndex == 0) mutableListOf<MutableList<Double>>().also { newRows += it } else newRows[rowIndex]

					  row.mapIndexed { colIndex, pixel ->
						val newCol =
						  if (colorIndex == 0) mutableListOf<Double>().also { newRow += it } else newRow[colIndex]
						newCol += pixel
					  }
					}
					//					it.map { it.map { it.toDoubleArray() } }
				  }
				  newRows
				}


			  )
			}

			val neurons = (0 until top.numNeurons).map {
			  Neuron(
				index = it,
				top100 = top.top100[it].map { images[it]!! }
			  )
			}.toObservable()
			resultBox.clear()
			resultBox.apply {
			  label("Layer ID: ${top.layerID}")
			  label("Layer Name: ${top.layerName}")
			  label("Num Neurons: ${top.numNeurons}")
			  var cb: ChoiceBoxWrapper<Neuron>? = null
			  hbox {
				label("choose neuron: ")
				cb = choicebox(values = neurons)
			  }
			  swapper(cb!!.valueProperty) {
				VBoxWrapper().apply {
				  val im = top100[0]
				  canvas(width = im.matrix[0].size.toDouble(), height = im.matrix.size.toDouble()) {
					val pw = graphicsContext2D.pixelWriter
					im.matrix.forEachIndexed { y, row ->
					  row.forEachIndexed { x, pix ->
						val r = maxOf(minOf((pix[0] + 1)/2, 1.0), 0.0)
						val g = maxOf(minOf((pix[1] + 1)/2, 1.0), 0.0)
						val b = maxOf(minOf((pix[2] + 1)/2, 1.0), 0.0)
						pw.setColor(
						  x, y, FXColor.rgb((r*255.0).roundToInt(), (g*255.0).roundToInt(), (b*255.0).roundToInt())
						)
					  }
					}
				  }
				  vgrow = ALWAYS
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


