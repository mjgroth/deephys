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
        state = [[[0.5]]]
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
        pixel_data = [[[0.5], [0.5], [0.5]]]
        test = dp.import_test_data(
            name="test",
            classes=classes,
            pixel_data=pixel_data,
            ground_truths=[0],
            state=state,
            model=model,
        )
        pixel_data = torch.zeros([32, 32, 3], dtype=torch.int32)
        test = dp.import_test_data(
            name="test",
            classes=classes,
            pixel_data=pixel_data,
            ground_truths=[0],
            state=state,
            model=model,
        )
        test.save()


if __name__ == "__main__":
    unittest.main()
