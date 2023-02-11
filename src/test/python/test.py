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
        model = dp.model(
            "model", {"layer2": num_classes, "layer1": 1, "layer3": 12}, classification_layer="layer2"
        )
        model.save()
        state = {"layer1": [[0.5] * 1] * num_images,
                 "layer2": [[0.5] * num_classes] * num_images,
                 "layer3": [[0.5] * 12] * num_images,
            }

        pixel_data = np.zeros([num_images, 3, 32, 32])

        ground_truths = np.array([0] * num_images)
        test = dp.export(
            dataset_name="test",
            category_names=classes,
            images=pixel_data,
            groundtruth_categories=ground_truths,
            neural_activity=state,
            model=model,
        )
        test.save()
        pixel_data = pixel_data.tolist()
        test = dp.export(
            dataset_name="test2",
            category_names=classes,
            images=pixel_data,
            groundtruth_categories=ground_truths,
            neural_activity=state,
            model=model,
        )
        test.save()


if __name__ == "__main__":
    unittest.main()
