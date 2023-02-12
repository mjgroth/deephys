package matt.nn.deephys.gui.visbox

import javafx.application.Platform
import javafx.geometry.Pos.TOP_CENTER
import matt.file.toSFile
import matt.fx.base.prop.sizeProperty
import matt.fx.graphics.dialog.openFile
import matt.fx.graphics.wrapper.node.NodeWrapper
import matt.fx.graphics.wrapper.node.disableWhen
import matt.fx.graphics.wrapper.node.visibleAndManagedWhen
import matt.fx.graphics.wrapper.pane.anchor.swapper.swap
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.pane.vbox.v
import matt.lang.go
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.dsetsbox.DSetViewsVBox
import matt.nn.deephys.gui.global.DEEPHYS_FONT_MONO
import matt.nn.deephys.gui.global.deephyButton
import matt.nn.deephys.gui.global.deephyCheckbox
import matt.nn.deephys.gui.global.deephysSingleCharButtonFont
import matt.nn.deephys.gui.global.deephysText
import matt.nn.deephys.gui.global.titleFont
import matt.nn.deephys.gui.global.tooltip.SUFFIX_WARNING
import matt.nn.deephys.gui.global.tooltip.deephyTooltip
import matt.nn.deephys.gui.global.tooltip.symbol.DEEPHYS_SYMBOL_SPACING
import matt.nn.deephys.gui.global.tooltip.symbol.deephysInfoSymbol
import matt.nn.deephys.gui.global.tooltip.symbol.deephysWarningSymbol
import matt.nn.deephys.gui.modelvis.ModelVisualizer
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.init.modelBinding
import matt.nn.deephys.load.loadSwapper
import matt.nn.deephys.state.DeephyState
import matt.obs.bind.binding
import matt.obs.prop.BindableProperty
import matt.prim.str.mybuild.string
import matt.prim.str.truncateWithElipsesOrAddSpaces


class VisBox(
  app: DeephysApp,
  settings: DeephysSettingsController,
): VBoxW() {




  init {

	/*val settingsButton = settButton*/

	/*val prefButtonHeight = settingsButton.heightProperty*/
	val prefButtonHeight = BindableProperty(25.0)

	spacing = 10.0
	alignment = TOP_CENTER

	val visualizer = BindableProperty<ModelVisualizer?>(null)
	val pleaseLoadModelToSeeVisualizerText = "Choose a model in order to visualize it"
	val visualizerToolTipText = BindableProperty(pleaseLoadModelToSeeVisualizerText)
	val showVisualizer = BindableProperty(false)

	h {
	  spacing = 25.0




	  deephyButton("Choose Model") {
		prefHeightProperty.bind(prefButtonHeight)
		setOnAction {


		  val f = openFile {
			extensionFilter("model files", "*.model")
		  }?.toSFile()

		  if (f != null) {
			DeephyState.tests.value = null
			DeephyState.model.value = f
		  }
		}
	  }

	  h {

		deephyTooltip(
		  visualizerToolTipText,
		  settings = settings
		) /*tooltip has to be outside of checkbox or else it will not show when checkbox is disabled?*/


		deephyCheckbox("Show Model Diagram") {
		  prefHeightProperty.bind(prefButtonHeight)
		  visualizer.onChange {
			if (it == null) {
			  isSelected = false
			}
		  }
		  disableWhen { visualizer.isNull }

		  showVisualizer.bind(selectedProperty)
		}
	  }

/*	  h {
		hgrow = ALWAYS
		alignment = Pos.CENTER_RIGHT
		+settingsButton
	  }*/

	}



	v {
	  visibleAndManagedWhen { showVisualizer }
	  swap(visualizer)
	}


	loadSwapper(modelBinding.await(), nullMessage = "Select a .model file to begin") {
	  val model = this@loadSwapper
	  VBoxWrapperImpl<NodeWrapper>().apply {

		h {
		  spacing = 10.0
		  deephysText("Model: ${model.name}") {
			titleFont()
		  }

		  h {
			spacing = DEEPHYS_SYMBOL_SPACING
			deephysInfoSymbol(
			  string {
				lineDelimited {
				  +"Layers"
				  model.layers.forEach {
					+"\t${it.layerID.truncateWithElipsesOrAddSpaces(15)}: ${it.neurons.size}"
				  }
				}
			  }
			) {
			  fontProperty v DEEPHYS_FONT_MONO
			  /*wrapTextProp v true*/
			}
			if (model.wasLoadedWithSuffix) {
			  deephysWarningSymbol(SUFFIX_WARNING)
			}
		  }

		}


		println("loaded model: " + model.infoString())

		val maxNeurons = 50


		val vis = if (model.layers.all { it.neurons.size <= maxNeurons }) {
		  visualizerToolTipText v "Show an interactive diagram of the model"
		  ModelVisualizer(model, settings)
		} else {
		  visualizerToolTipText v "model is too large to visualize (>$maxNeurons in a layer)"
		  null
		}
		visualizer v vis

		val dSetViewsBox = DSetViewsVBox(model, settings)
		dSetViewsBox.modelVisualizer = vis
		vis?.dsetViewsBox = dSetViewsBox
		val theTests = DeephyState.tests.value
		theTests?.go {
		  dSetViewsBox += it
		}
		+dSetViewsBox
		deephyButton("Add a test") {
		  deephysSingleCharButtonFont()
		  textProperty.bind(dSetViewsBox.children.sizeProperty.binding {
			if (it == 0) "Add a test" else "+"
		  })
		  deephyTooltip("Add a test", settings = settings)
		  setOnAction {
			dSetViewsBox.addTest()
		  }

		}
		Platform.runLater {
		  app.testReadyDSetViewsBbox.page(dSetViewsBox)
		}
	  }
	}

  }
}