Changelist
==========

0.12.0
~~~~~~

- changed function and parameter names, most notably `import_test_data` -> `test`
- `neural_activity` dict parameter which is more robust and user friendly than previous `state` parameter
- documentation updates, including new `terminology` page
- pip package no longer requires pytorch. Test still uses and requires pytorch. parameters can still take torch data as input, but this is no longer shown explicitly in the docs.
- multiple new checks and errors thrown to ensure that exported data is issue-free
- updated homepage, documentation URL, pip project description
- removed pytorch import function

0.11.0
~~~~~~

- `classification_layer` argument

0.10.0
~~~~~~

- Removed `suffix` params
- `classes` params bug fix: images no longer need to contain at least 1 of each class
- `model` function
- Updated docs
- added `tqdm` for data formatting
