import unittest
import sys
import os
import torch
import torchvision

sys.path.append(os.path.join(os.path.dirname(__file__), "..", "..", "main", "python"))
import deephys as dp


class TestStringMethods(unittest.TestCase):
    def test_deephys(self):
        testset = torchvision.datasets.CIFAR10(
            root="./data", train=False, download=True, transform=transforms.ToTensor()
        )
        testloader = torch.utils.data.DataLoader(
            testset, batch_size=args["batch_size"], shuffle=False, num_workers=2
        )
        model = dp.Model("model", None, [])
        state = []
        classes = ["dog"]
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
