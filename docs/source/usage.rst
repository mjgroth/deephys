  Usage
=====

.. _installation:

Installation
------------

To use Deephys, first install it using pip:

.. code-block:: console

   (.venv) $ pip install deephys

Extracting Activations From Data
--------------------------------

To extract data from a test, please see the steps provided `here <https://colab.research.google.com/drive/1vOfau2lS004ilX6aIAASMKhFnzfi3uQ-#scrollTo=Ky2zpklwpN1W>`_

Here ``act_extract`` function takes dataloader and model as parameters. For example: 

>>> all_activs, all_outputs, all_images, all_cats = act_extract(testloader, models)

Here ``all_activs`` is the 2D float array of neurons and activations of the penultimate layer.

``all_outputs`` is the 2D float array of neurons and logits for the classification layer.

``all_outputs`` is an ordered list of image pixel data [images,channels,dim1,dim2] containing information of all images of testloader.

``all_cats`` is an ordered list of ground truths.

Generating Data For Deephys
---------------------------
you can use the :py:func:`deephys.import_test_data` function:

Parameter ``name`` in :func:`deephys.import_test_data` should be a string containing i.e. the name of the dataset.

Parameter ``state`` in :func:`deephys.import_test_data` should be a 3D float array layers, neurons, and activations respectively.

Parameter ``classes`` in :func:`deephys.import_test_data` should be an ordered list of strings representing class names.

Parameter ``model`` in :func:`deephys.import_test_data` should be the model structure.

Parameter ``pixel_data`` in :func:`deephys.import_test_data` should be an ordered list of image pixel data [images,channels,dim1,dim2].

Parameter ``ground_truths`` in :func:`deephys.import_test_data` should be an ordered list of ground truths.

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
