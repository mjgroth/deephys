Changelist
==========

0.14.0
~~~~~~~~~~~~

- Throw an error if ``category_names`` is not a ``list``
  
- change ``model_name`` parameter back to ``name``
  
- Rename class ``Test`` to ``DatasetActivity`` and function `test` to ``dataset_activity``
  
0.13.0
~~~~~~~~~~~~

- Allow saving files to any path
  
- Change name of ``ImageFile`` to ``Image``
  
0.12.1
~~~~~~~~~~~~

- Fixed the ``importlib.util`` bug
  
- Note: You can still use ``0.12.0`` if you add ``import importlib.util`` to the top of your scripts
  
0.12.0
~~~~~~~~~~~~

- changed function and parameter names, most notably `import_test_data` -> `test`
  
- `neural_activity` dict parameter which is more robust and user friendly than previous `state` parameter
  
- documentation updates, including new `terminology` page
  
- pip package no longer requires pytorch. Test still uses and requires pytorch. parameters can still take torch data as input, but this is no longer shown explicitly in the docs.
  
- multiple new checks and errors thrown to ensure that exported data is issue-free
  
- updated homepage, documentation URL, pip project description
  
- removed pytorch import function
  
0.11.0
~~~~~~~~~~~~

- `classification_layer` argument
  
0.10.0
~~~~~~~~~~~~

- Removed `suffix` params
  
- `classes` params bug fix: images no longer need to contain at least 1 of each class
  
- `model` function
  
- Updated docs
  
- added `tqdm` for data formatting
  