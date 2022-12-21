<!--- https://github.com/mgroth0/deephys/releases -->

[//]: # (VERSION:1.23.1)


Compatible pip package
version: [0.6.0](https://pypi.org/project/deephys/0.6.0/) ([instructions](https://colab.research.google.com/drive/1aR5lnpVMxda7wUj1RZ6YODX5N2FA8YRn))

[//]: # (### PIP Python Package Updated to 0.6.0)
[//]: # (### New Features)

### Performance Improvements
- Slightly faster loading time
  - removed redundant threads
- Reduced memory usage
    - predictions: LinkedHashMap with no capacity -> HashMap with initial capacity
- Fixed memory leaks from
  - `layoutProxy`
  - `activationsByNeuron`
  - `tooltips`
  - `neuronListViewSwapper`
  - `ModelVisualizer`
- greatly improved data file loading times by making all caches into RAFs

[//]: # (### Cosmetic Changes)

### Bug Fixes
- Fixed a "file load stream broken" bug
- Increased max JavaFX vram from 500MB to 2GB to avoid some bugs

### Internal Development
- During tests, app now automatically navigates between many images and categories to simulate prolonged usage of the app. This will help identify memory leaks.
- Tests now affirm that loading times are below 5 seconds for CIFARX2 and 30 seconds for INX3


[//]: # (### New Tests)
[//]: # (### Notes)
[//]: # (### Todo)