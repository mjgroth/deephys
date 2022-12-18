package matt.nn.deephys.model.importformat.im

import matt.fx.graphics.wrapper.style.FXColor
import matt.lang.anno.PhaseOut
import matt.lang.weak.lazyWeak
import matt.model.flowlogic.latch.asyncloaded.DelegatedSlot
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.nn.deephys.calc.act.RawActivation
import matt.nn.deephys.load.test.PixelData3
import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.InterTestLayer
import matt.nn.deephys.model.data.InterTestNeuron
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test
import matt.nn.deephys.model.importformat.mstate.ModelState
import java.lang.ref.WeakReference

class DeephyImage(
  val imageID: Int,
  categoryID: Int,
  category: String,
  val activations: ModelState,
  val testLoader: TestLoader,
  val index: Int,
  val model: Model,
  val features: Map<String, String>?,
  test: LoadedValueSlot<Test>,
) {


  val category = Category(id = categoryID, label = category)

  val matrix by lazyWeak {
	val d = data.await()
	val numRows = d[0].size
	val numCols = d[0][0].size

	(0 until numRows).map { index1 ->
	  MutableList<FXColor>(numCols) { index2 ->
		FXColor.rgb(d[0][index1][index2], d[1][index1][index2], d[2][index1][index2])
	  }
	}
  }


  internal val weakActivations by lazyWeak {
	activations.activations.await()
  }

  fun activationsFor(rLayer: InterTestLayer): FloatArray = weakActivations[rLayer.index]
  fun activationFor(neuron: InterTestNeuron) = RawActivation(weakActivations[neuron.layer.index][neuron.index])

  val data = DelegatedSlot<PixelData3>()

  @PhaseOut
  private val weakTest = WeakReference(test)

  val prediction by lazy {
	weakTest.get()!!.await().preds.await()[this]!!
  }

}
