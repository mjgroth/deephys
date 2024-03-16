package matt.nn.deephys.load.test.testloadertwo

import matt.nn.deephys.load.test.PostDtypeTestLoader
import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.testcache.TestRAMCache
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TypedTestLike

class PreppedTestLoader<N: Number>(
    val tl: PostDtypeTestLoader<N>,
    override val dtype: DType<N>
): TypedTestLike<N> {
    override fun numberOfImages(): ULong = tl.numberOfImages()

    override fun imageAtIndex(i: Int): DeephyImage<N> = tl.awaitImage(i)


    override val test: Test<N> get() = tl.awaitFinishedTest()

    override fun isDoneLoading(): Boolean = tl.isDoneLoading()

    override val testRAMCache: TestRAMCache get() = tl.testRAMCache
    override val model: Model get() = tl.model
    override val post: PostDtypeTestLoader<N>
        get() = tl
}

