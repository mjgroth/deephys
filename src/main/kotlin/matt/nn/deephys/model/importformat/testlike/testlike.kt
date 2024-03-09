package matt.nn.deephys.model.importformat.testlike

import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.testcache.TestRAMCache
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test
import matt.nn.deephys.model.importformat.im.DeephyImage

interface TestOrLoader {
    val test: Test<*>
    val testRAMCache: TestRAMCache
    val dtype: DType<*>
    val model: Model
    fun isDoneLoading(): Boolean
}



interface TypedTestLike<A: Number>: TestOrLoader {
    fun numberOfImages(): ULong
    fun imageAtIndex(i: Int): DeephyImage<A>
    override val test: Test<A>
    override val dtype: DType<A>
}
