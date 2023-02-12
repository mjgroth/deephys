[![pip](https://img.shields.io/badge/compatible%20pip%20version-0.12.0-00bbe2?&logo=pypi&logoColor=f5c39e)](https://pypi.org/project/deephys/0.12.0)


### New Features
- Alert the user with a popup when they open the app if their settings were reset
- New much more organized settings window
- Added Navigation Box with links to useful URLs
- TopCategories table for each Neuron
- Image spinner
-"tab" buttons to change between different views and indicate which one we are currently in
- New layout of CategoryView contains some small new visual features




### User Friendliness
- default popup duration moved from 5 seconds to 1 second
- settings are reset for every user, so if they had an old version where the popups where inifinite by default they will now be reset to 1 second
- Tried to create a less dramatic warning symbol
- Use percentages for accuracies
- More clear label in ByCategory view above the top average neurons
- "Bind" capitalized


### Removed Features
- Removed "select random image" since it is replaced by a better image spinner
- Removed image ID label under image in ByImage view since it is now in the spinner


### Cosmetic Changes
- Adjusted symbol spacing
- Revised layout of category view


### Bug Fixes
- Fixed a bug causing tooltips to not work properly
- Fixed a bug causing the word "tooltip" to display as "matt.fx.tooltip...." (something like that)
- App previously crashed or froze when activation data or images data was wrong shape. Helpful error messages were added to alert the user that the loaded data is the wrong shape without crashing the app.
- Fixed bug where the category view choicebox was not registering into history (back button did not work)







### Notes
- python PIP changelists from now on will be posted to the documentation here:  https://deephys.readthedocs.io/en/latest/changelist.html
- These release notes here now will only pertain to the app, not the python library

