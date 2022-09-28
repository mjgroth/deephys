<!--- https://github.com/mgroth0/deephy/releases -->

[//]: # (VERSION:1.16.0)


Compatible pip package
version: [0.5.3](https://pypi.org/project/deephy/0.5.3/) ([instructions](https://colab.research.google.com/drive/1HAaVOopHDNVKryP14wW4K_rcqeeqYrLK#scrollTo=VtUgz8xGYKHj))

[//]: # (### PIP Python Package Updated to 0.5.0)

### New Features
- Added new setting to control to which decimal point prediction values are rounded to
- Added tooltips explaining what the numbers mean for neuron all activations
- Added symbols to mark what the different activations are
  - `%`means ratio to the bound test
  - `Y` means raw activation
  - `Ŷ` means normalized activation (divided by max for this neuron for all images)
- Added the new Model Visualizer
- Added the new Category View

### Performance Improvements
- Added new calculation caching system. Certain operations have their parameters and results cached to avoid recomputing the same thing.

### Cosmetic Changes
- Changed tooltip style to improve readability

[//]: # (### Bug Fixes)


### Notes
- Imported data must now include a classification layer for the app to work properly (previously it was just reduced functionality, now an error will be thrown)
- ImageNet results with "Texture: " in category label will no longer work. We will need a better data structure to support this. This will come in a later update and will require small changes to the data.

[//]: # (### Todo)

