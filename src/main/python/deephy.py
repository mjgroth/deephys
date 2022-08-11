"""
This class was written to make sharing data between python and kotlin easier.
The following cell shows example usage.
I have already tested that the data correctly loads into kotlin.
"""
# https://www.rfc-editor.org/rfc/rfc8949.html
from cbor2 import dump
from dataclasses import dataclass, asdict
from typing import List

@dataclass
class ImageFile:
  id: int
  categoryID: int
  category: str
  data: List[List[List[float]]]

@dataclass
class Neuron:
  activations: List[float] # one float per image (10000 length)

@dataclass
class Layer:
  id: str
  name: str
  neurons: List[Neuron] # 50 neurons


@dataclass
class DeephyCborData:
  datasetName: str #CIFAR10 #CIFAR10V2
  suffix: str = ""

  layers: List[Layer]
  images: List[ImageFile]


  def save(self):

    if self.suffix == "":
      fileName = self.datasetName + '.cbor'
    else:
      fileName = self.datasetName + "_" + self.suffix + '.cbor'

    with open(fileName, 'wb') as fp:
      dump(asdict(self), fp)

      #CIFAR10.cbor