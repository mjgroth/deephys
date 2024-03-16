package matt.nn.deephys.gui.fix

import matt.collect.set.contents.Contents
import matt.collect.set.contents.contentsOf
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TypedTestLike


fun <A: Number> DeephyImage<A>.withTest() = testLoader.withImage(this)

fun <A: Number> TypedTestLike<A>.withNoImages() = withImages(contentsOf())

fun <A: Number> TypedTestLike<A>.withImage(image: DeephyImage<A>) = withImages(contentsOf(image))


fun <A: Number> TypedTestLike<A>.withImages(images: Contents<DeephyImage<A>>): TestAndSomeImages<A> = TestAndSomeImages(test = this, images = images)




data class TestAndSomeImages<A: Number>(
    val images: Contents<DeephyImage<A>>,
    val test: TypedTestLike<A>
)
