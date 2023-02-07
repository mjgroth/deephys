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
        layer1 = dp.Layer(layerID="layer1", neurons=[dp.Neuron()])
        layer2 = dp.Layer(layerID="layer2", neurons=[dp.Neuron(), dp.Neuron()])
        model = dp.Model("model", [layer1, layer2], classification_layer="layer2")
        model2 = dp.model(
            "model", {"layer1": 1, "layer2": 2}, classification_layer="layer2"
        )
        state = [[[0.5]] * num_images, [[0.5, 0.5]] * num_images]
        print(f"state={np.array(state).shape}")
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
        torch_test = dp.import_torch_dataset(
            name="torch_test",
            dataset=testloader.dataset,
            classes=classes,
            state=state,
            model=model,
        )
        torch_test.save()
        pixel_data = np.zeros([num_images, 3, 32, 32])
        ground_truths = [0] * num_images
        test = dp.import_test_data(
            name="test",
            classes=classes,
            pixel_data=pixel_data,
            ground_truths=ground_truths,
            state=state,
            model=model,
        )
        pixel_data = pixel_data.tolist()
        test = dp.import_test_data(
            name="test",
            classes=classes,
            pixel_data=pixel_data,
            ground_truths=ground_truths,
            state=state,
            model=model,
        )
        pixel_data = torch.zeros([num_images, 3, 32, 32])
        test = dp.import_test_data(
            name="test",
            classes=classes,
            pixel_data=pixel_data,
            ground_truths=ground_truths,
            state=state,
            model=model,
        )
        test.save()
        test = dp.import_test_data(
            name="test",
            classes=classes,
            pixel_data=pixel_data,
            ground_truths=ground_truths,
            state=state,
            model=model2,
        )
        test.save()


if __name__ == "__main__":
    unittest.main()
