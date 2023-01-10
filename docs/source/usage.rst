Usage
=====

.. _installation:

Installation
------------

To use Deephys, first install it using pip:

.. code-block:: console

   (.venv) $ pip install deephys

Extracting Activations
----------------------

To extract data from a test,
you can use the :py:func:`deephys.deephys.import_torch_dataset` function:

Parameter ``state`` in :func:`deephys.deephys.import_torch_dataset` should be a 3D float array layers, neurons, and activations respectively.

For example:

>>> test_data_2 = np.transpose(test_data['images'], (0, 3, 1, 2))/255.
>>> test_data_2 = TensorDataset(torch.FloatTensor(test_data_2), torch.LongTensor(test_data['labels']))
>>> testloader = torch.utils.data.DataLoader(test_data_2,
    batch_size=args['batch_size'], shuffle=False, **kwargs)
>>> testV2 = import_torch_dataset(
    "CIFARV2",
    testloader.dataset,
    classes,
    [all_activs_2,all_outputs_2],
    model
    )
>>> test.suffix = None
>>> testV2.save()
The data is now saved to a file called "CIFARV2.test"

Please see `here <https://github.com/mjgroth/deephys-aio/blob/master/Python_Tutorial.ipynb>`_ for the full tutorial
