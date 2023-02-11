import unittest
import sys
import os
import numpy as np

sys.path.append(os.path.join(os.path.dirname(__file__), "..", "..", "main", "python"))
import deephys as dp


class TestDeephys(unittest.TestCase):
    def test_deephys(self):

        num_images = 100
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
        model = dp.Model("model", [layer1, layer2], classification_layer="layer2")
        model2 = dp.model(
            "model", {"layer2": num_classes, "layer1": 1}, classification_layer="layer2"
        )
        model2.save()
        state = {"layer1": [[0.5]] * num_images,
                 "layer2": [[0.5] * num_classes] * num_images
            }

        pixel_data = np.zeros([num_images, 3, 32, 32])

        ground_truths = np.array([0] * num_images)
        test = dp.export(
            name="test",
            classes=classes,
            pixel_data=pixel_data,
            ground_truths=ground_truths,
            state=state,
            model=model,
        )
        test.save()
        pixel_data = pixel_data.tolist()
        test = dp.export(
            name="test",
            classes=classes,
            pixel_data=pixel_data,
            ground_truths=ground_truths,
            state=state,
            model=model,
        )
        test.save()


if __name__ == "__main__":
    unittest.main()
