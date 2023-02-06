[//]: # (VERSION:1.30.0)
[![pip](https://img.shields.io/badge/compatible%20pip%20version-0.9.2-00bbe2?&logo=pypi&logoColor=f5c39e)](https://pypi.org/project/deephys/0.9.2)


### New Features
- Neuron spinner now wraps around (e.g. hitting down on neuron 0 will go to neuron 50 and up on neuron 50 will go to neuron 0)


### Performance Improvements
- Fixed some memory leaks
-TopNeurons loads faster because loading bar popup was removed
- Reduced memory and cpu usage from image progress indicators
- Fixed a set of major memory leaks in the category pie charts. App would previously cease to a halt after prolonged use of the category view, but now it is much more robust.
- Greatly reduced the CPU usage from reference queues (low level improvement that will make app feel more smooth throughout)


### User Friendliness
  - Formatted ActivationRatios as percentages (multiplied them by 100 and put the `%` sign after)
  - Formatted activation tooltips as latex
  - Updated and more clear tooltips for activations
  - Activations highlight on hover (to encourage user to see tooltip)
  - cursor changes to hand when hovering over image
  - improved tip below pie charts


### Removed Features
- Removed the "BindTutorial" (this tutorial became a pointless waste of space)
- Removed the "suffix" field for objects saved in python (this was useless and users can easily add a suffix as part of the name)
- Removed the loading bar pop up for TopNeuron views. It was actually slowing down the loading process. Also, we have made enough performance gains that this is no longer worth it.


### Cosmetic Changes
  - New tooltips
  - Info symbol text highlight in blue instead of yellow in light mode
  - Made '+' and '-' button bigger
  - new look for info circles


### Bug Fixes
  - Maybe fixed version checker (can't check until new update)
  - Corrected the tooltip for the ActivationRatio calculation
  - Fixed a bug causing activation ratios to show in the ByNeuron view even after normalizer is un-selected
  - Fixed a bug causing raw activations to show in the category view of the "bind" dataset even if it was set as "
    Normalizer". Now activation ratios will correctly show.
  - Fixed bug where it said "there are no top neurons due to 'zero' activations" when really it was just because no normalizer was set
  - Fixed bug causing TopImages of a neuron to only show the top 18. Now it shows the top 100.
  - Fixed a crash that happened sometimes when the layer was switched. It was caused by the new layer not having a neuron at the currently selected index (e.g. when neuron 50 was selected and then the classification layer was selected, which only has 10 neurons.) This crash was solved by catching this case and setting the neuron to 0.
  - Stopped saving data in ~/registered with data related to window locations and preffered monitors. They are now properly stored in OS-specific properties
  - Fixed an issue that caused settings or states to sometimes not save
  - App now takes new "classes" field in `.test` files to prevent errors when not all categories are included as ground truths of images. This requires a newer pip package version.
  - Fixed a bug causing error logs to not be saved into files.
  - Prevented deadlock and freeze caused by image progress indicators
  - Fixed a set of major memory leaks coming from the category pie charts






### Notes
- I ran out of time and could not release the newest python version with this. Python version 0.9.2 will still work with this version, but as soon as the newer python version is released please switch to that. You will see warnings about switching to a newer python version. Unfortunately these warnings are un-actionable untl I release the new python version.

