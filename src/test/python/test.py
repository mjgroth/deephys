import unittest
import sys
import os

sys.path.append(os.path.join(os.path.dirname(__file__), "..", "..", "main", "python"))
import deephys as dp


class TestStringMethods(unittest.TestCase):
    def test_deephys(self):
        test = dp.import_test_data(
            "test",
            [[[0.5], [0.5], [0.5]]],
            [0],
            ["dog"],
            [],
            dp.Model("model", None, []),
        )
        test.save()


if __name__ == "__main__":
    unittest.main()
