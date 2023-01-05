import unittest
import sys
import os
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
        model = dp.Model("model", None, [])
        state = []
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
            "torch_test", testloader.dataset, classes, state, model
        )
        torch_test.save()
        test = dp.import_test_data(
            "test", [[[0.5], [0.5], [0.5]]], [0], classes, state, model
        )
        test.save()


if __name__ == "__main__":
    unittest.main()
