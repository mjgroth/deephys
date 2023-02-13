[![pip](https://img.shields.io/badge/compatible%20pip%20version-0.12.0-00bbe2?&logo=pypi&logoColor=f5c39e)](https://pypi.org/project/deephys/0.12.0)


### New Features
- Add maximum raw activation to Neuron view when there is no Normalizer
- TopCategories can show normalized activations
- Added Category Spinner




### User Friendliness
- Improved message when there are no TopNeurons showing




### Cosmetic Changes
- Improved look of spinner when bad input is entered


### Bug Fixes
- Since the "Normalizer" functino was added to the app (used to be merged with the "Bind " function), the Image view has not correctly shown top neurons and activations in the case where there is a "bind" selected but not a "Normalizer". This is now fixed.
- Fixed a bug where the app crashed instead of showing an error popup







