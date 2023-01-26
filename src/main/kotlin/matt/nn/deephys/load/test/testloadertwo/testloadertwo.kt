package matt.nn.deephys.load.test.testloadertwo

import matt.nn.deephys.load.test.TestLoader
import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.testcache.TestRAMCache
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TypedTestLike

class PreppedTestLoader<N: Number>(
  val tl: TestLoader,
  override val dtype: DType<N>
): TypedTestLike<N> {
  override fun numberOfImages(): ULong {
	return tl.numImages.await()
  }

  override fun imageAtIndex(i: Int): DeephyImage<N> {
	@Suppress("UNCHECKED_CAST")
	return tl.awaitImage(i) as DeephyImage<N>
  }

  @Suppress("UNCHECKED_CAST")
  override val test: Test<N> get() = tl.awaitFinishedTest() as Test<N>
  override val testRAMCache: TestRAMCache get() = tl.testRAMCache
  override val model: Model get() = tl.model
}

