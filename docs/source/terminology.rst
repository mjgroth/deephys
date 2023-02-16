==============
📕 Terminology
==============

Here we define terms that may help to clarify other sections of the documentation, as well as function names and labels throughout our software.

🧬 Model
========

A `Model` in Deephys is a neural network, such as AlexNet.

🧪 DatasetActivity
==================

A `DatasetActivity` in Deephys represents a single model, a single dataset, and the neural activations that the model experienced when it was given this dataset as input.

Lead
====

The "Lead" is the current DatasetActivity that is "leading" the others inside of the visualizer. This mean that wherever you go in this data, whichever neuron image or category, the others will follow.

Normalizer
==========

The "Normalizer" is the current DatasetActivity that is being used as the normalizer inside of neuronal activation calculations. This means that activations for all neurons, both in this DatasetActivity and others, will have their activations divided by the maximum activation of the respective neuron in the "Normalizer".
