# @title Deephy
from cbor2 import dump
from dataclasses import dataclass, asdict
from typing import List, Optional
from typing import List, Optional
import numpy
import torch
import struct
from time import time

# library already optimizes writes of int8
float32 = lambda val: struct.pack("!f", val)


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
            subLen = len(sub) / 4
            neuronsLen = len(self.layers[index].neurons)
            if subLen != neuronsLen:
                raise Exception(
                    f"expected activations data with length of {neuronsLen} (number of neurons in layer {index}) but got {subLen / 4}"
                )
        ms = self.ModelState(activations)
        return ms

    @dataclass
    class ModelState:
        activations: List[bytearray]  # float32


def import_torch_dataset(name, dataset, classes, state):
    imageList = []
    for i in range(len(dataset)):
        image, target = dataset[i]
        if torch.is_tensor(target):
            target = target.item()
        image = image * 255
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
        chan_to_bytes = lambda chan: [bytes(row) for row in chan]
        im_to_bytes = lambda im: list(map(chan_to_bytes, im))
        im_as_list = image.numpy().astype(numpy.uint8).tolist()
        im_activations = list(map(lambda x: x[i, :].tolist(), state))
        imageList.append(
            ImageFile(
                imageID=i,
                categoryID=target,
                category=classes[target],
                data=im_to_bytes(im_as_list),
                # python cbor package has no way to make float32, also bytearray is smaller/faster
                activations=model.state(
                    list(
                        map(
                            lambda layer: bytearray(
                                [b for a in layer for b in float32(a)]
                            ),
                            im_activations,
                        )
                    )
                ),
            )
        )
    test = Test(name=name, suffix=None, images=imageList)
    return test


@dataclass
class ImageFile:
    imageID: int
    categoryID: int
    category: str
    data: List[bytearray]  # R8G8B8
    activations: Model.ModelState


@dataclass
class Test(DeephyData):
    images: List[ImageFile]

    def __post_init__(self):
        self.extension = f"test"


class Stopwatch:
    def __init__(self, name):
        self.name = name

    def start(self):
        self.start_time = time()

    def stop(self):
        self.stop_time = time()
        self.duration_secs = self.stop_time - self.start_time

    def report(self):
        print(f"{self.name} took {self.duration_secs} seconds")

    def stop_and_report(self):
        self.stop()
        self.report()


def start_stopwatch(name):
    sw = Stopwatch(name)
    sw.start()
    return sw