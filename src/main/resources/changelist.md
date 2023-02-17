[![pip](https://img.shields.io/badge/compatible%20pip%20version-0.13.0-00bbe2?logo=pypi&logoColor=f5c39e)](https://pypi.org/project/deephys/0.13.0)


### New Features
- Add maximum raw activation to Neuron view when there is no Normalizer
- TopCategories can show normalized activations
- Added Category Spinner
- Added favicons for URL icons
- Added "give me a demo" button
- Added neuron zoo version 1
- link for submitting bug reports
- Feedback button




### User Friendliness
- Improved message when there are no TopNeurons showing
- When there are no images with a certain category, explain this with a message rather than saying the accuracy is "NaN"
- Change "Bind" button to say "Lead"


### Removed Features
- Model Diagram is removed (for now). Maybe this will be added later. The cost of maintaining this feature is currently not worth its value.


### Cosmetic Changes
- Improved look of spinner when bad input is entered
- Improved layout by using more of the empty space
- Smaller latex tooltips


### Bug Fixes
- Since the "Normalizer" functino was added to the app (used to be merged with the "Bind " function), the Image view has not correctly shown top neurons and activations in the case where there is a "bind" selected but not a "Normalizer". This is now fixed.
- Fixed a bug where the app crashed instead of showing an error popup
- Switch ground truth title to a label that does not wrap. This prevents a layout bug that was caused from when the ground truth category name is really long and wraps, which used to mess up the layout
- Fixed bug that occured when there were very few images in a .test file (less than 5)
- prevent some crashes that can occur from errors when loading a file. Now the app is better at displaying an error message without crashing.
- Font now correctly applies to ChoiceBoxes







