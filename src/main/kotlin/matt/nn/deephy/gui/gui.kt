@file:OptIn(ExperimentalSerializationApi::class)

package matt.nn.deephy.gui

import javafx.scene.control.ContentDisplay
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import matt.file.CborFile
import matt.file.construct.toMFile
import matt.file.toSFile
import matt.fx.graphics.FXColor
import matt.hurricanefx.eye.bind.toStringConverter
import matt.hurricanefx.eye.lang.Prop
import matt.hurricanefx.eye.lib.onChange
import matt.hurricanefx.eye.prop.objectBinding
import matt.hurricanefx.eye.prop.stringBinding
import matt.hurricanefx.tornadofx.item.choicebox
import matt.hurricanefx.wrapper.canvas.CanvasWrapper
import matt.hurricanefx.wrapper.pane.titled.TitledPaneWrapper
import matt.hurricanefx.wrapper.pane.vbox.VBoxWrapper
import matt.hurricanefx.wrapper.parent.parent
import matt.hurricanefx.wrapper.text.TextWrapper
import matt.hurricanefx.wrapper.wrapped
import matt.nn.deephy.model.DeephyData
import matt.nn.deephy.model.DeephyImage
import matt.nn.deephy.model.FileNotFound
import matt.nn.deephy.model.ParseError
import matt.nn.deephy.state.DeephyState
import kotlin.math.roundToInt
import matt.stream.message.FileList



class DSetViewsVBox: VBoxWrapper() {

}

@InternalSerializationApi
class DatasetViewer(initialFile: CborFile? = null): TitledPaneWrapper() {
  private val fileProp: Prop<CborFile?> = Prop(initialFile).apply {
	onChange {
	  DeephyState.datasets.value = parent!!.getChildList()!!
		.map { it.wrapped() as DatasetViewer }
		.mapNotNull { it.fileProp.value?.toSFile() }.let { FileList(it) }
	  println("DeephyState.datasets=${DeephyState.datasets.value}")
	}
  }
  private val dataBinding = fileProp.objectBinding {
	if (it != null && it.doesNotExist) FileNotFound
	else {
	  it?.let {
		Cbor.decodeFromByteArray<DeephyData>(it.readBytes())
	  }
	}
  }

  init {


	contentDisplay = ContentDisplay.LEFT
	isExpanded = true
	titleProperty.bind(
	  fileProp.stringBinding { it?.nameWithoutExtension }
	)
	graphic = hbox {
	  button("remove dataset") {
		tooltip("remove this dataset viewer")
		setOnAction {
		  this@DatasetViewer.removeFromParent()
		}
	  }
	  button("select dataset") {
		tooltip("choose dataset file")
		setOnAction {

		  val f = FileChooser().apply {
			title = "choose data folder"
			this.extensionFilters.setAll(ExtensionFilter("cbor", "*.cbor"))
		  }.showOpenDialog(stage)

		  if (f != null) {
			this@DatasetViewer.fileProp.value = f.toMFile() as CborFile
		  }
		}
	  }

	}
	content = swapper(dataBinding, nullMessage = "select a dataset to view it") {
	  when (this) {
		is FileNotFound -> TextWrapper("${fileProp.value} not found")
		is ParseError   -> TextWrapper("parse error")
		is DeephyData   -> VBoxWrapper().apply {
		  val layerCB = choicebox(values = layers)
		  hbox {
			text("layer: ")
			+layerCB
		  }
		  swapper(layerCB.valueProperty, nullMessage = "select a layer") {
			VBoxWrapper().apply {
			  val neuronCB = choicebox(values = neurons.withIndex().toList()) {
				converter = toStringConverter { "neuron ${it?.index}" }
			  }
			  hbox {
				text("neuron: ")
				+neuronCB
			  }
			  swapper(neuronCB.valueProperty, nullMessage = "select a neuron") {
				VBoxWrapper().apply {
				  flowpane {
					(0 until 100).forEach { imIndex ->
					  val im = images[value.activationIndexesHighToLow[imIndex]]
					  canvas {
						draw(im)
					  }
					  vgrow = Priority.ALWAYS
					}
				  }

				}
			  }
			}
		  }
		}
	  }

	}.node
  }
}

fun CanvasWrapper.draw(image: DeephyImage) {
  node.width = image.matrix[0].size.toDouble()
  node.height = image.matrix.size.toDouble()
  val pw = graphicsContext2D.pixelWriter
  image.matrix.forEachIndexed { y, row ->
	row.forEachIndexed { x, pix ->
	  val r = pix[0]
	  val g = pix[1]
	  val b = pix[2]
	  pw.setColor(
		x, y, FXColor.rgb((r*255.0).roundToInt(), (g*255.0).roundToInt(), (b*255.0).roundToInt())
	  )
	}
  }
}

@Deprecated(
  "this was for when pixel values were at weird values like -2.0 to 2.0 which Anirban confimed happens in some datasets (one of the CIFARs). It is buggy, obviously."
) fun CanvasWrapper.drawOld(image: DeephyImage) {
  node.width = image.matrix[0].size.toDouble()
  node.height = image.matrix.size.toDouble()
  val pw = graphicsContext2D.pixelWriter
  image.matrix.forEachIndexed { y, row ->
	row.forEachIndexed { x, pix ->
	  val r: Double = maxOf(minOf((pix[0] + 1)/2, 1.0), 0.0)
	  val g: Double = maxOf(minOf((pix[1] + 1)/2, 1.0), 0.0)
	  val b: Double = maxOf(minOf((pix[2] + 1)/2, 1.0), 0.0)

	  val fxColor = FXColor.rgb((r*255.0).roundToInt(), (g*255.0).roundToInt(), (b*255.0).roundToInt())

	  pw.setColor(
		x, y, fxColor
	  )
	}
  }
}