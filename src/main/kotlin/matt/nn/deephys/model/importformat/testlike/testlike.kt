package matt.nn.deephys.model.importformat.testlike

import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.testcache.TestRAMCache
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test

interface TestOrLoader {
  val test: Test<*>
  val testRAMCache: TestRAMCache
  val dtype: DType<*>
  val model: Model
}

interface TypedTestLike<A: Number>: TestOrLoader {
  override val test: Test<A>
  override val dtype: DType<A>
}