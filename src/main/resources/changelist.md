[//]: # (VERSION:1.29.1)
[![pip](https://img.shields.io/badge/compatible%20pip%20version-0.10.0-00bbe2?&logo=pypi&logoColor=f5c39e)](https://pypi.org/project/deephys/0.10.0)
### PIP Python Package Updated to 0.10.0







### User Friendliness
  - Formatted ActivationRatios as percentages (multiplied them by 100 and put the `%` sign after)
  - Formatted activation tooltips as latex
  - Updated and more clear tooltips for activations
  - Activations highlight on hover (to encourage user to see tooltip)
  - cursor changes to hand when hovering over image


### Cosmetic Changes
  - New tooltips
  - Info symbol text highlight in blue instead of yellow in light mode


### Bug Fixes
  - Maybe fixed version checker (can't check until new update)
  - Corrected the tooltip for the ActivationRatio calculation
  - Fixed a bug causing activation ratios to show in the ByNeuron view even after normalizer is un-selected
  - Fixed a bug causing raw activations to show in the category view of the "bind" dataset even if it was set as "
    Normalizer". Now activation ratios will correctly show.
  - Fixed bug where it said "there are no top neurons due to 'zero' activations" when really it was just because no normalizer was set
  - Fixed bug causing TopImages of a neuron to only show the top 18. Now it shows the top 100.







