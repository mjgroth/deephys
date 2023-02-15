==============
📕 Terminology
==============

Here we define terms that may help to clarify other sections of the documentation, as well as function names and labels throughout our software.

🧬 Model
========

A `Model` in Deephys is a neural network, such as AlexNet.

💽 Dataset
==========

A `Dataset` in Deephys represents a collection of images (input data). These objects are not currently created directly because the app is currently designed expecting there to be a one to one mapping between Datasets and Tests. We may sometimes casually refer to a `Test` as a `Dataset` because we are usually testing different datasets side by side, but technically the software is capable of testing the same dataset side by side in different conditions (such as at different training epochs). Therefore, we try to distinguish the terms whenever possible.

🧪 Test
=======

A `Test` in Deephys represents a single model, a single dataset, and the neural activations that the model experienced when it was given this dataset as input.
