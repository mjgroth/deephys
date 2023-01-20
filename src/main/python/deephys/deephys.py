# @title DEEPHYS
from cbor2 import dump
from dataclasses import dataclass, asdict
from typing import List, Optional, Mapping
import numpy as np
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
class DEEPHYSData:
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
class Model(DEEPHYSData):
    """

    :param name: name of the model (e.g.: `"resnet18"`)
    :type name: str
    :param suffix: optional suffix for the model
    :type suffix: Optional[str]
    :param layers: A List of layers in the model. The app is guaranteed to support cases where there are 2 layers and one of them is called `"classification"`.
    :type layers: List[deephys.deephys.Layer]
    """

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


def import_torch_dataset(name, dataset, classes, state, model):
    """
    Conveniently calls import_test_data with PyTorch data.

    :param name: the name of the dataset
    :type name: str
    :param dataset: contains pixel data of images
    :type dataset: torch.utils.data.DataLoader
    :param classes: an ordered list of strings representing class names
    :type classes: list
    :param state: a 3D array of floats [layers,neurons,activations]. Length of activations must be the same as the number of images.
    :type state: Union[list,np.ndarray]
    :param model: the model structure
    :type model: deephys.deephys.Model
    :return: a formatted data object which may be saved to a file
    :rtype: deephys.deephys.Test
    """
    pixelDataList = []
    groundTruthList = []
    for i in range(len(dataset)):
        image, target = dataset[i]
        if torch.is_tensor(target):
            target = target.item()
        pixelDataList.append(image)
        groundTruthList.append(target)
    pixelDataList = torch.stack(pixelDataList)
    return import_test_data(
        name=name,
        pixel_data=pixelDataList,
        ground_truths=groundTruthList,
        classes=classes,
        state=state,
        model=model,
    )


def import_test_data(name, classes, state, model, pixel_data, ground_truths):
    """
    Prepare test results for Deephys

    :param name: the name of the dataset
    :type name: str
    :param classes: an ordered list of strings representing class names
    :type classes: list
    :param state: a 3D array of floats [layers,neurons,activations]. Length of activations must be the same as the number of images.
    :type state: Union[list,np.ndarray]
    :param model: the model structure
    :type model: deephys.deephys.Model
    :param pixel_data: an ordered list of image pixel data [images,channels,dim1,dim2]. Pixels must be floats within the range 0.0:1.0
    :type pixel_data: Union[list,np.ndarray,torch.FloatTensor]
    :param ground_truths: an ordered list of ground truths
    :type ground_truths: List[int]
    :return: a formatted data object which may be saved to a file
    :rtype: deephys.deephys.Test
    """
    imageList = []
    if isinstance(state, list):
        state = np.array(state)
    if isinstance(pixel_data, list):
        pixel_data = torch.tensor(pixel_data)
    elif isinstance(pixel_data, np.ndarray):
        pixel_data = torch.from_numpy(pixel_data)
    if len(ground_truths) != len(pixel_data):
        raise Exception(
            f"ground_truths length ({len(ground_truths)}) must be same as the image length ({len(pixel_data)})"
        )
    for i in range(len(pixel_data)):
        image = pixel_data[i]
        target = ground_truths[i]
        mn = torch.min(image)
        mx = torch.max(image)
        if mx > 1 or mn < 0:
            raise Exception(
                f"image pixel values must be floats between 0 and 1, but values given range from {mn} to {mx}"
            )
        image = image * 255
        chan_to_bytes = lambda chan: [bytes(row) for row in chan]
        im_to_bytes = lambda im: list(map(chan_to_bytes, im))
        im_as_list = image.numpy().astype(np.uint8).tolist()
        im_activations = state[:, :, i]
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
                features=None,
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
    features: Optional[Mapping[str, str]]
    activations: Model.ModelState


@dataclass
class Test(DEEPHYSData):
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
        self.stop(null)
        self.report(null)


def start_stopwatch(name):
    sw = Stopwatch(name)
    sw.start()
    return sw
