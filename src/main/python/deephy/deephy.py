"""
https://www.rfc-editor.org/rfc/rfc8949.html
"""
from cbor2 import dump
from dataclasses import dataclass, asdict
from typing import List, Optional
import numpy

@dataclass
class ImageFile:
  imageID: int
  categoryID: int
  category: str
  data: List[List[List[float]]]

  def __post_init__(self):
    n = numpy.array(self.data)
    theMax = numpy.max(n)
    if theMax > 1.0:
      raise Exception("image pixel values should be between 0.0 and 1.0, but a value of " + str(theMax) + " was recieved")
    theMin = numpy.min(n)
    if theMin < 0.0:
      raise Exception("image pixel values should be between 0.0 and 1.0, but a value of " + str(theMin) + " was recieved")

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