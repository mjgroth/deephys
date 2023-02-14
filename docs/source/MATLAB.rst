MATLAB
======

.. note::
	MATLAB support is experimental and incubating.

Exporting Neural Activity from MATLAB
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To export data from MATLAB, you will need to call python from MATLAB.

The following minimal example generates random data and exports it. The data generated here is compatible with the app. It is up to you to implement this into your MATLAB deep learning pipeline.

.. code-block:: matlab

	clear;
	% Only call this once per MATLAB session
	% env = pyenv("Version","/path/to/python");
	penultimate_size = 10;
	classification_size = 2;
	layers = struct("penultimate",py.int(penultimate_size),"classification",py.int(classification_size));
	model = py.deephys.model("resnet",layers,"classification");
	model.save()
	num_ims = 4;
	activity = struct("penultimate",py.numpy.array(rand(num_ims,penultimate_size)),"classification",py.numpy.array(rand(num_ims,classification_size)));
	images = {};
	for i = 1:num_ims
	    images = [images {py.numpy.array(rand(3,64,64))}];
	end
	images = py.list(images);
	groundtruth = {py.int(0),py.int(1),py.int(0),py.int(1)};
	test = py.deephys.test("InD",{"cat","dog"},activity,model,images,groundtruth);
	test.save()
