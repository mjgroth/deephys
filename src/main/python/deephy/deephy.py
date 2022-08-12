"""
https://www.rfc-editor.org/rfc/rfc8949.html
"""
from cbor2 import dump
from dataclasses import dataclass, asdict
from typing import List, Optional

@dataclass
class ImageFile:
  imageID: int
  categoryID: int
  category: str
  data: List[List[List[float]]]

@dataclass
class Neuron:
  activations: List[float] # one float per image (10000 length)

@dataclass
class Layer:
  layerID: str
  neurons: List[Neuron] # 50 neurons


@dataclass
class DeephyData:
  datasetName: str #CIFAR10 #CIFAR10V2
  suffix: Optional[str]
  images: List[ImageFile]
  layers: List[Layer]

  def save(self):

    if self.suffix == None:
      fileName = self.datasetName + '.cbor'
    else:
      fileName = self.datasetName + "_" + self.suffix + '.cbor'

    with open(fileName, 'wb') as fp:
      dump(asdict(self), fp)

      #CIFAR10.cbor