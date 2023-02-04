Wrapping up your data 
=============

Before starting, install our Deephys wrapper Python library. This library faciliates plugging your data with the right format for Deephys.

.. code-block:: console

  $ pip install deephys


Now, let's get started wrapping up your data  🚀

☀️ Setting up the model parameters
--------------------------------------

We first need to set up the model parameters to visualize. The output layer always needs to be included. Any number of layers can be included too.  To set up the model, we need to indicate the name of the model, as well as the number of neurons per layer and the name of the layer. This can be simply done in the following way: 

>>> import deephys
>>> model = deephys.setup_model(
    num_neurons = [num_neurons_layer, num_neurons_output],
    name_layers = ["penultimate_layer", "output_layer"]
    name_network = "my_net"
    )
    
This will create a `.model` file that will be useful for Deephys. In this example, we only set up two layers. If you want to visualize more layers, add them  in the list.


‼️ The last entry in both lists must be the output layer, which is mandatory to always have.


🎏 Wrap up each dataset distributions separatelly 🎏
--------------------------------------

Each dataset distribution that you would like to analyze needs to be wrapped-up separatelly. After wrapping up all dataset distributions, you can visualize them together using Deephys 🪄.  

🤔 How to wrap up one dataset distribution? It is just the following 2 steps:

1. Extract images 🖼️, categories 🐕, and neural activity from the network 🔥🔥🔥
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Here is all what you need:

- ``images`` 🖼️: It is advisable to resize them to a small size (eg. 64x64 pixels) to save sapce. Dimensions: [#images,#channels,H,W].
- ``groundtruth_categories`` 🐕: Integer indicating the ground-truth label number. Dimensions: [#images].
- ``caregory_names`` 🎈: Strings indicating the name of each category. Dimensions: [#categories].
- ``network_output`` 🔥: For each image, obtain the output of the network (after or before the softmax, it is up to you). Dimensions: [#images, #categories].
- ``neural_activity`` 🔥🔥🔥: For each image, obtain the neural activity that you want to visualize. This can be obtained for as many layers as you want. If you want to visualize a convolutional layer or a transformer, please see this for options. Dimensions: [#image, #neurons].

You can keep them in a python list or a numpy array. 

‼️ Make sure that the order of the images is kept constant across all the data.

2. Wrap-up the data with the Deephys wrapper ✨
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We are now ready to save. Just plug all the data to our Deephys wrapper in the following format. 

>>> test = deephys.import_test_data(
    name = "OOD",
    pixel_data = images,
    ground_truths = groundtruth_categories,
    classes = caregory_names,
    state = [neural_activity, network_output],
    model = model
    )
>>> test.suffix = None
>>> test.save()

Note that ``mdoel`` is the model that was created at the beginning. The wrapper create a file called ``OOD.test``, which can used in Deephys.

You can add more layers to the visualization by just adding them in the state list, just make sure `network_output` is the last one.

🎏 Remember to follow step 1 and 2 for each dataset distribution separatelly.


.. Extracting Activations From Data


Examples 
--------------------------------------

To extract data from a test, please see the steps provided `here <https://colab.research.google.com/github/mjgroth/deephys-aio/blob/master/Python_Tutorial.ipynb>`_

Here ``act_extract`` function takes dataloader and model as parameters. For example: 

>>> all_activs, all_outputs, all_images, all_cats = act_extract(testloader, models)

Here ``all_activs`` is the 2D float array of neurons and activations of the penultimate layer.

``all_outputs`` is the 2D float array of neurons and logits for the classification layer.

``all_outputs`` is an ordered list of image pixel data [images,channels,dim1,dim2] containing information of all images of testloader.

``all_cats`` is an ordered list of ground truths.

Generating Data For Deephys
---------------------------
you can use the :py:func:`deephys.deephys.import_test_data` function:

Parameter ``name`` in :func:`deephys.deephys.import_test_data` should be a string containing i.e. the name of the dataset.

Parameter ``state`` in :func:`deephys.deephys.import_test_data` should be a 3D float array layers, neurons, and activations respectively.

Parameter ``classes`` in :func:`deephys.deephys.import_test_data` should be an ordered list of strings representing class names.

Parameter ``model`` in :func:`deephys.deephys.import_test_data` should be the model structure.

Parameter ``pixel_data`` in :func:`deephys.deephys.import_test_data` should be an ordered list of image pixel data [images,channels,dim1,dim2].

Parameter ``ground_truths`` in :func:`deephys.deephys.import_test_data` should be an ordered list of ground truths.


Please see `here <https://github.com/mjgroth/deephys-aio/blob/master/Python_Tutorial.ipynb>`_ for the full tutorial

