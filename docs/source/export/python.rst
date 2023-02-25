===========
From Python
===========

Python
======

`See the Pip Package <https://pypi.org/project/deephys/>`_

Before starting, install our Deephys exporter Python library. This library facilitates encapsulating your data into a format compatible with Deephys and saves the data to files.

.. code-block:: console

  pip install deephys

Now, let's get started exporting your data  🚀

☀️ Defining Your Model
======================

We first need to define the model to visualize. Any number of layers to be visualized can be included. To define the model indicate its name, the neuron count per layer. The classification (output) layer needs to be also indicated as it needs to be always included in the visualization.

.. code-block:: python

  import deephys as dp
  
  dp_model = dp.model(
      name="my_net",
      layers={  # include any additional layers
          "penultimate_layer": num_neurons_layer,
          "output": num_neurons_output,
      },
      classification_layer="output",
  )
  dp_model.save()
  

This will create a ``my_net.model`` to be imported into Deephys.

🎏 Export each dataset distributions separately 🎏
==================================================

Each dataset distribution that you would like to analyze needs to be exported separately. After exporting all dataset distributions, you can visualize them together using Deephys 🪄.

🤔 How to export one dataset distribution? It is just the following 2 steps:

1. Extract images 🖼️, categories 🐕, and neural activity 🔥🔥🔥
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Here is all the variables we need to extract for each data distribution:

- ``images``: They need to be in the range [0, 1]. It is advisable to resize them to a small size (eg. 64x64 pixels) to save space and memory. They can be color or grayscale images. Dimensions: ``[#images,#channels,H,W]``.
  
- ``groundtruth``: Integer indicating the ground-truth label number for each image. Dimensions: ``[#images]``.
  
- ``neural_activity``: For each image, extract the neural activity that you want to visualize. The neural activity needs to be extracted for each layers we indicated in the model that we want to visualize. Dimensions for each layer: ``[#images, #neurons]``.
  
All these variables can be Python lists or numpy arrays.

🤯 IMPORTANT: Make sure that the order of the images is aligned with the order of the groundtruth and the neural activity.
🤯 If you want to visualize a convolutional layer or a transformer, please see this for options (TBD).

We will also need the ``category_names``, which is a list of strings indicating the name of each category (Dimensions: ``[#categories]``).

2. Convert the data to a Deephys-compatible format ✨
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We are now ready to convert the data in a Deephys-compatible format. Just plug all the variables obtained in step 1 to our Deephys export function in the following format:

.. code-block:: python

  test = dp.dataset_activity(
      name="Data_Distribution_1",
      category_names=category_names,
      images=images,
      groundtruth=groundtruth,
      neural_activity={
          "penultimate_layer": neural_activity_penultimate,
          "output": neural_activity_output,
      },
      model=dp_model,
  )
  distribution.save()
  

Note that ``dp_model`` is the model that was defined at the beginning of the process. Also, note that the neural activity extracted for each layer has been placed in a dictionary that indicates from what layer it has been extracted.

Finally, ``distribution.save()`` will create a file called ``Data_Distribution_1.test``, which can visualized in Deephys.

🎏 Remember to follow step 1 and 2 for each dataset distribution separately. This will generate a different visualization file for each distribution that can then be visualized in Deephys all together.
