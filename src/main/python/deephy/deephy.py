from cbor import dump
from dataclasses import dataclass, asdict
from typing import List, Optional
from typing import List, Optional
import numpy
import torch
import struct
import cbor

cbor.dumps_float = lambda val: struct.pack("!Bf", cbor.CBOR_FLOAT64, val)


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
            fileName = f"{self.name}.{self.extension}"
        else:
            fileName = f"{self.name}_{self.suffix}.{self.extension}"
        with open(fileName, "wb") as fp:
            dump(asdict(self), fp)


@dataclass
class Model(DeephyData):
    layers: List[Layer]

    def __post_init__(self):
        self.extension = f"model"

    def state(self, activations):
        if len(activations) != len(self.layers):
            raise Exception(
                f"expected activations data with length of {len(self.layers)} (number of layers) but got {len(activations)}"
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


def import_torch_dataset(name, dataset, classes, state):
    imageList = []
    for i in range(len(dataset)):
        image, target = dataset[i]
        mn = torch.min(image)
        mx = torch.max(image)
        if mx > 255:
            raise Exception(
                f"image pixel values should be integers between 0 and 255, but a value of {mx} was received"
            )
        if mn < 0:
            raise Exception(
                f"image pixel values should be integers between 0 and 255, but a value of {mn} was received"
            )
        chan_to_byte = lambda chan: int(chan)
        px_to_bytes = lambda px: bytes(list(map(chan_to_byte, px)))
        row_to_bytes = lambda row: [b for px in row for b in px_to_bytes(px)]
        im_to_bytes = lambda im: list(map(row_to_bytes, im))
        im_as_list = image1.numpy().tolist()
        imageList.append(
            ImageFile(
                imageID=i,
                categoryID=target,
                category=classes[target],
                data=im_to_bytes(im_as_list),
                activations=state,
            )
        )
    test = Test(name=name, suffix=None, images=imageList)
    return test


@dataclass
class ImageFile:
    imageID: int
    categoryID: int
    category: str
    data: List[bytearray]
    activations: Model.ModelState

    def __post_init__(self):
        n = numpy.array(self.data)
        theMax = numpy.max(n)
        if theMax > 1.0:
            raise Exception(
                f"image pixel values should be between 0.0 and 1.0, but a value of {theMax} was received"
            )
        theMin = numpy.min(n)
        if theMin < 0.0:
            raise Exception(
                f"image pixel values should be between 0.0 and 1.0, but a value of {theMin} was received"
            )


@dataclass
class Test(DeephyData):
    images: List[ImageFile]

    def __post_init__(self):
        self.extension = f"test"