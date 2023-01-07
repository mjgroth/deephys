<!--- https://github.com/mgroth0/deephys/releases -->

[//]: # (VERSION:1.23.1)


Compatible pip package
version: [0.7.2](https://pypi.org/project/deephys/0.7.2/) ([instructions](https://colab.research.google.com/drive/1vOfau2lS004ilX6aIAASMKhFnzfi3uQ-))

### PIP Python Package Updated to 0.7.2
- Set up first python tests to better ensure bug-free python updates
- Added `import_test_data` function

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

### Cosmetic Changes
- Added app logo to .app, dock, and title bar

### Bug Fixes
- Fixed a "file load stream broken" bug
- Increased max JavaFX vram from 500MB to 2GB to avoid some bugs

### Internal Development
- During tests, app now automatically navigates between many images and categories to simulate prolonged usage of the app. This will help identify memory leaks.
- Tests now affirm that loading times are below 5 seconds for CIFARX2 and 30 seconds for INX3


[//]: # (### New Tests)

### Notes
- MIT License Added

[//]: # (### Todo)

[//]: # (CHANGELIST CANDIDATES)