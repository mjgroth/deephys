from cbor import dump
from dataclasses import dataclass, asdict
from typing import List, Optional
from typing import List, Optional
import numpy


@dataclass
class Neuron:
    pass


@dataclass
class Layer:
    layerID: str
    neurons: List[Neuron]


@dataclass
class DeephyData:
    name: str
    suffix: Optional[str]

    def save(self):
        if self.suffix == None:
            fileName = self.name + "." + self.extension
        else:
            self.name = self.name + "_" + self.suffix + "." + self.extension
        with open(fileName, "wb") as fp:
            dump(asdict(self), fp)


@dataclass
class Model(DeephyData):
    layers: List[Layer]

    def __post_init__(self):
        self.extension = "model"

    def state(self, activations):
        if len(activations) != len(self.layers):
            raise Exception(
                "expected activations data with length of "
                + str(len(self.layers))
                + " (number of layers) but got "
                + str(len(activations))
            )
        for index, sub in enumerate(activations):
            subLen = len(sub)
            neuronsLen = len(self.layers[index].neurons)
            if subLen != neuronsLen:
                raise Exception(
                    f"expected activations data with length of {neuronsLen} (number of neurons in layer {index}) but got {subLen}"
                )
        ms = self.ModelState(activations)
        return ms

    @dataclass
    class ModelState:
        activations: List[List[float]]


@dataclass
class ImageFile:
    imageID: int
    categoryID: int
    category: str
    data: List[List[List[float]]]
    activations: Model.ModelState

    def __post_init__(self):
        n = numpy.array(self.data)
        theMax = numpy.max(n)
        if theMax > 1.0:
            raise Exception(
                "image pixel values should be between 0.0 and 1.0, but a value of "
                + str(theMax)
                + " was received"
            )
        theMin = numpy.min(n)
        if theMin < 0.0:
            raise Exception(
                "image pixel values should be between 0.0 and 1.0, but a value of "
                + str(theMin)
                + " was received"
            )


class Test(DeephyData):
    images: List[ImageFile]

    def __post_init__(self):
        self.extension = "test"