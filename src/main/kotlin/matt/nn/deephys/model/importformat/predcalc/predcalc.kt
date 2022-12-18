package matt.nn.deephys.model.importformat.predcalc

/*

class PredictionsCalculator(
  test: Test
) {

  private val weakTest = WeakReference(test)
  private val ims = test.images
  private val testName = test.name
  private val clsLayerIndex = test.model.classificationLayer.index

  fun calculate(): Map<DeephyImage,Category> {
	val localCatsByID = weakTest.get()!!.catsByID
	val m = mutableMapOf<DeephyImage, Category>()
	val chunkSize = 1000
	ims.chunked(chunkSize).forEachIndexed { chunkIndex, imageChunk ->
	  val actsMat = imageChunk.map {
		it.weakActivations[clsLayerIndex].asList()
	  }.toNDArray()
	  val argMaxResults = mk.math.argMaxD2(actsMat, 1)
	  val imageStartIndex = chunkIndex*chunkSize
	  argMaxResults.forEachIndexed { imageIndex, predIndex ->
		m[ims[imageStartIndex + imageIndex]] = localCatsByID[predIndex]!!
	  }
	  throttle("PredictionsCalculator of $testName")
	}

	*/
/*val argMaxResults = mk.math.argMaxD2(activationsMatByLayerIndex[model!!.classificationLayer.index], 1)
	argMaxResults.forEachIndexed { imageIndex, predIndex ->
	  m[images[imageIndex]] = category(predIndex)
	}*//*


	return m
  }


}*/
