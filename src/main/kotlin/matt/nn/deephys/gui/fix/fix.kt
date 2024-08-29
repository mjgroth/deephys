package matt.nn.deephys.gui.fix

import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TypedTestLike


fun <A: Number> DeephyImage<A>.withTest() = testLoader.withImage(this)

fun <A: Number> TypedTestLike<A>.withNoImages() = withImages(setOf())

fun <A: Number> TypedTestLike<A>.withImage(image: DeephyImage<A>) = withImages(setOf(image))


fun <A: Number> TypedTestLike<A>.withImages(images: Set<DeephyImage<A>>): TestAndSomeImages<A> =
    TestAndSomeImages(
        test = this,
        images = images
    )




data class TestAndSomeImages<A: Number>(
    val images: Set<DeephyImage<A>>,
    val test: TypedTestLike<A>
)
