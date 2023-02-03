
Before starting, install our Deephys wrapper. This will faciliate plugging your data in the right format for Deephys.

.. code-block:: console

  $ pip install deephys


Let's get started wrapping up your data to visualize with Deephys  🚀

☀️ Save the model configuration parameters
==================

We first need to instantiate the model by indicating the number of neurons per layer to visualize.

>>> import deephys
>>> model = deephys.setup_model(
    num_neurons = [num_neurons_layer, num_neurons_output],
    name_layers = ["penultimate_layer", "output_layer"]
    name_network = "my_net"
    )
    
If you want to visualize more layers, add the number of neurons and parameters in the list.

‼️ The last entry in both lists must be the output layer, which is mandatory to always have.


🎏 Wrap up dataset distributions separatelly 🎏
===========================================

Each dataset distribution that you would like to analyze needs to be wrapped-up independently. After wrapping up all dataset distributions, you can visualize them together using Deephys 🪄.  

🤔 How to wrap up one dataset distribution? It is just the following 2 steps:

1. Extract images 🖼️, categories 🐕, and neural activity from the network 🔥🔥🔥
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can keep them in a python list or a numpy array. Here is all what you need:

- images 🖼️: It is advisable to resize them to a small size (eg. 64x64 pixels) to save sapce. Dimensions: [#images,#channels,H,W].
- categories 🐕: Integer indicating the ground-truth label number. Dimensions: [#images].
- caregory names 🎈: Strings indicating the name of each category. Dimensions: [#categories].
- output of the network 🔥: For each image, obtain the output of the network (after or before the softmax, it is up to you). Dimensions: [#images, #categories].
- neural activity 🔥🔥🔥: For each image, obtain the neural activity that you want to visualize. This can be obtained for as many layers as you want. If you want to visualize a convolutional layer or a transformer, please see this for options. Dimensions: [#image, #neurons].

‼️ Make sure that the order of the images is kept constant across all the data.

2. Wrap-up the data with the Deephys wrapper ✨
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We are now ready to save. Just plug all the data to our Deephys wrapper in the following format. 

>>> test = deephys.import_test_data(
    name = "OOD",
    pixel_data = images,
    ground_truths = gt_categories,
    classes = category_names,
    state = [neural_activity, network_output],
    model = model
    )
>>> test.suffix = None
>>> test.save()


Note that you will need the model that was created at the beginning. Also, you can add more layers to the visualization by just adding them in the state list, just make sure `network_output` is the last one.

🎏 Remember to follow step 1 and 2 for each dataset distribution separatelly.


EXAMPLES

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

For example:

>>> test = import_test_data(
    name = "CIFAR10",
    classes = classes,
    state = [all_activs,all_outputs],
    model = model,
    pixel_data = all_images,
    ground_truths = all_cats.numpy().tolist()
    )
test.suffix = None
>>> test.suffix = None
>>> test.save()
The data is now saved to a file called "CIFAR10.test"

Please see `here <https://github.com/mjgroth/deephys-aio/blob/master/Python_Tutorial.ipynb>`_ for the full tutorial

