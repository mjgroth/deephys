package matt.nn.deephys.model.importformat.testlike

import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.testcache.TestRAMCache
import matt.nn.deephys.model.importformat.Test

interface TestOrLoader {
  val test: Test<*>
  val testRAMCache: TestRAMCache
  val dtype: DType<*>
}

interface TypedTestLike<A: Number>: TestOrLoader {
  override val test: Test<A>
  override val testRAMCache: TestRAMCache
  override val dtype: DType<A>
}