package matt.nn.deephys.gui.viewer.action

import matt.nn.deephys.gui.dataset.DatasetNodeView
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.im.DeephyImage

sealed interface TestViewerAction
class SelectImage(val image: DeephyImage<*>): TestViewerAction
class SelectNeuron(val neuron: InterTestNeuron): TestViewerAction
class SelectCategory(val cat: CategorySelection): TestViewerAction
class SelectView(val view: DatasetNodeView): TestViewerAction