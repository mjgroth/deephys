# @title DEEPHYS
from cbor2 import dump
from dataclasses import dataclass, asdict
from typing import List, Optional, Mapping, Union
import numpy as np
import torch
import struct
from time import time

# library already optimizes writes of int8
# python cbor package has no way to make float32, also bytearray is smaller/faster
float32 = lambda val: struct.pack("!f", val)
float64 = lambda val: struct.pack("!d", val)


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
    :param suffix: optional suffix for the model
    :param layers: A List of layers in the model. The app is guaranteed to support cases where there are 2 layers and one of them is called `"classification"`.
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


def import_torch_dataset(
    name: str,
    dataset: torch.utils.data.DataLoader,
    classes: list,
    state: Union[list, np.ndarray, torch.FloatTensor],
    model: Model,
    dtype: str = "float32",
):
    """
    Conveniently calls import_test_data with PyTorch data.

    :param name: the name of the dataset
    :param dataset: contains pixel data of images
    :param classes: an ordered list of strings representing class names
    :param state: a 3D array of floats [layers,activations,neurons]. Length of activations must be the same as the number of images.
    :param model: the model structure
    :param dtype: The data type to save activation data as: "float32" or "float64". "float64" is more precise but results in data files almost twice as large. "float64" may also be slower in the app. The input type does not matter, it will get converted to the type in this argument. Default: "float32")
                  Default: ``"float32"``
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


def _to_np(d):
    if isinstance(d, list):
        return np.array(d)
    if torch.is_tensor(d):
        return d.cpu().detach().numpy()
    return d


def _to_torch(value):
    if isinstance(value, list):
        return torch.tensor(value)
    elif isinstance(value, np.ndarray):
        return torch.from_numpy(value)
    return value


def _to_list(value):
    if isinstance(value, (np.ndarray, torch.IntTensor)):
        return value.tolist()
    return value


def import_test_data(
    name: str,
    classes: list,
    state: Union[list, np.ndarray, torch.FloatTensor],
    model: Model,
    pixel_data: Union[list, np.ndarray, torch.FloatTensor],
    ground_truths: Union[List[int], np.ndarray, torch.IntTensor],
    dtype: str = "float32",
):
    """
    Prepare test results for Deephys

    :param name: the name of the dataset
    :param classes: an ordered list of strings representing class names
    :param state: a 3D array of floats [layers,activations,neurons]. Length of activations must be the same as the number of images.
    :param model: the model structure
    :param pixel_data: an ordered list of image pixel data [images,channels,dim1,dim2] or [images,dim1,dim2] for greyscale. Pixels must be floats within the range 0.0:1.0
    :param ground_truths: an ordered list of ground truths. The length should be the same as the number of images. Each element should be an integer indicating the index of the class.
    :param dtype: The data type to save activation data as: "float32" or "float64". "float64" is more precise but results in data files almost twice as large. "float64" may also be slower in the app. The input type does not matter, it will get converted to the type in this argument. Default: "float32")
                  Default: ``"float32"``
    :return: a formatted data object which may be saved to a file
    :rtype: deephys.deephys.Test
    """
    imageList = []
    state = list(map(_to_np, state))
    pixel_data = _to_torch(pixel_data)
    if len(pixel_data.shape) == (3):
        pixel_data = torch.unsqueeze(pixel_data, 1)
        pixel_data = pixel_data.repeat(1, 3, 1, 1)
    if isinstance(ground_truths, torch.FloatTensor):
        raise Exception(
            f"ground_truths should be a list-like of ints, but got a FloatTensor. Please make it an IntTensor."
        )
    ground_truths = _to_list(ground_truths)
    if len(pixel_data.shape) != (4):
        raise Exception(
            f"pixel_data should be a 3D or 4D array-like collection. Colored: [images,channels,dim1,dim2] or greyscale: [images,dim1,dim2], but a shape of {pixel_data.shape} was received"
        )
    if len(ground_truths) != len(pixel_data):
        raise Exception(
            f"ground_truths length ({len(ground_truths)}) must be same as the image length ({len(pixel_data)})"
        )
    if pixel_data.shape[1] != 3:
        raise Exception(
            f"pixel_data should have 3 color channels, but {pixel_data.shape[1]} were received"
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
        im_activations = list(map(lambda l: l[i, :], state))
        if dtype == "float32":
            float_fun = float32
        elif dtype == "float64":
            float_fun = float64
        else:
            raise Exception(
                f"dtype must be 'float32' or 'float64'. Input was '{dtype}'"
            )
        imageList.append(
            ImageFile(
                imageID=i,
                categoryID=target,
                category=classes[target],
                data=im_to_bytes(im_as_list),
                activations=model.state(
                    list(
                        map(
                            lambda layer: bytearray(
                                [b for a in layer for b in float_fun(a)]
                            ),
                            im_activations,
                        )
                    )
                ),
                features=None,
            )
        )
    test = Test(name=name, suffix=None, dtype=dtype, images=imageList)
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
    dtype: Optional[str]
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
