import unittest
import sys
import os
import numpy as np
import torch
import torchvision
from torchvision import transforms

sys.path.append(os.path.join(os.path.dirname(__file__), "..", "..", "main", "python"))
import deephys as dp


class TestDeephys(unittest.TestCase):
    def test_deephys(self):
        os.chdir("/Users/matthewgroth/registered/tmp")
        testset = torchvision.datasets.CIFAR10(
            root="./data", train=False, download=True, transform=transforms.ToTensor()
        )
        testloader = torch.utils.data.DataLoader(
            testset, batch_size=128, shuffle=False, num_workers=2
        )
        num_images = len(testloader.dataset)
        classes = (
            "plane",
            "car",
            "bird",
            "cat",
            "deer",
            "dog",
            "frog",
            "horse",
            "ship",
            "truck",
        )
        num_classes = len(classes)
        layer1 = dp.Layer(layerID="layer1", neurons=[dp.Neuron()])
        layer2 = dp.Layer(layerID="layer2", neurons=[dp.Neuron()] * num_classes)
        layer3 = dp.Layer(layerID="layer3", neurons=[dp.Neuron()] * 12)
        model = dp.Model(
            "model", [layer1, layer2, layer3], classification_layer="layer2"
        )
        model2 = dp.model(
            "model",
            {"layer1": 1, "layer2": num_classes, "layer3": 12},
            classification_layer="layer2",
        )
        state = {
            "layer1": [[0.5] * 1] * num_images,
            "layer2": [[0.5] * num_classes] * num_images,
            "layer3": [[0.5] * 12] * num_images,
        }
        pixel_data = np.zeros([num_images, 3, 32, 32])
        ground_truths = [0] * num_images
        test = dp.test(
            name="test",
            category_names=classes,
            images=pixel_data,
            groundtruth=ground_truths,
            neural_activity=state,
            model=model,
        )
        pixel_data = pixel_data.tolist()
        test = dp.test(
            name="test",
            category_names=classes,
            images=pixel_data,
            groundtruth=ground_truths,
            neural_activity=state,
            model=model,
        )
        pixel_data = torch.zeros([num_images, 3, 32, 32])
        test = dp.test(
            name="test",
            category_names=classes,
            images=pixel_data,
            groundtruth=ground_truths,
            neural_activity=state,
            model=model,
        )
        test.save()
        test = dp.test(
            name="test",
            category_names=classes,
            images=pixel_data,
            groundtruth=ground_truths,
            neural_activity=state,
            model=model2,
        )
        test.save()
        test.save("my_file.test")
        test.save(path="my_file.test")


if __name__ == "__main__":
    unittest.main()
