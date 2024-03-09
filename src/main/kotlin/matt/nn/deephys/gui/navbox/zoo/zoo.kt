package matt.nn.deephys.gui.navbox.zoo

import matt.http.s3.rawS3Url
import matt.http.url.MURL


object NeuronalActivityZoo {


    val EXAMPLES =
        listOf(
            ZooExample(
                name = "CIFAR",
                modelURL =
                    zooURL(
                        path = "ActivityZoo/CIFAR10_Example/resnet18_cifar.model"
                    ),
                testURLs =
                    listOf(
                        zooURL("ActivityZoo/CIFAR10_Example/CIFAR10.test"),
                        zooURL("ActivityZoo/CIFAR10_Example/CIFARV2.test")
                    )
            ),
            ZooExample(
                name = "Colored MNIST",
                modelURL = zooURL("ActivityZoo/Colored_MNIST_Example/colored_mnist.model"),
                testURLs =
                    listOf(
                        zooURL("ActivityZoo/Colored_MNIST_Example/Colored_MNIST.test"),
                        zooURL("ActivityZoo/Colored_MNIST_Example/Permuted_colored_MNIST.test"),
                        zooURL("ActivityZoo/Colored_MNIST_Example/Arbitrary_colored_MNIST.test"),
                        zooURL("ActivityZoo/Colored_MNIST_Example/Drifted_colored_MNIST.test")
                    )
            ),
            ZooExample(
                name = "ImageNet ResNet18",
                modelURL = zooURL("ActivityZoo/ResNet18_ImageNet/resnet18_imagenet.model"),
                testURLs =
                    listOf(
                        zooURL("ActivityZoo/ResNet18_ImageNet/ImageNetV1.test"),
                        zooURL("ActivityZoo/ResNet18_ImageNet/ImageNetV2.test"),
                        zooURL("ActivityZoo/ResNet18_ImageNet/ImageNet_sketch.test"),
                        zooURL("ActivityZoo/ResNet18_ImageNet/ImageNet_style.test")
                    )
            ),
            ZooExample(
                name = "ImageNet Cvt",
                modelURL = zooURL("ActivityZoo/Cvt13/cvt13_imagenet.model"),
                testURLs =
                    listOf(
                        zooURL("ActivityZoo/Cvt13/ImageNetV1_cvt13.test"),
                        zooURL("ActivityZoo/Cvt13/ImageNetV2_cvt13.test"),
                        zooURL("ActivityZoo/Cvt13/ImageNet_sketch_cvt13.test"),
                        zooURL("ActivityZoo/Cvt13/ImageNet_style_cvt13.test")
                    )
            )
        )


    private fun zooURL(path: String) =
        rawS3Url(
            bucket = "deephys-tutorial-deps",
            path = path,
            region = "us-east-2"
        )
}

class ZooExample(
    val name: String,
    val modelURL: MURL,
    val testURLs: List<MURL>
)
