<!--- https://github.com/mgroth0/deephys/releases -->

[//]: # (VERSION:1.22.6)


Compatible pip package
version: [0.6.0](https://pypi.org/project/deephys/0.6.0/) ([instructions](https://colab.research.google.com/drive/1aR5lnpVMxda7wUj1RZ6YODX5N2FA8YRn))

[//]: # (### PIP Python Package Updated to 0.6.0)

[//]: # (### New Features)

### Performance Improvements
- More caching to hard drive (+ random access file cache)
- More throttling
- Fixed memory leaks causing removed test data not to be disposed from heap
  - Finally implemented weak listeners correctly
- Fixed caches that were not working because `ComputeInput` class was not `data`, so the cache could never be re-used
- Properly made `ComputeCaches` store their caches on `TestLoader`s whenever possible instead of globally
- Various other optimizations

### Cosmetic Changes
- Decreased minimum window width to accommodate small macbook air

### Bug Fixes
- Fixed major bug causing program to slow down because it thinks it is running out of RAM, when in fact it was not
- "Remove Test" now properly removed all test data from memory
- Fixed weak listeners not correctly being weak (as stated above)
- Fixed `ComputeInput` (as state above)

### Internal Development
- Added new tests to ensure that certain performance standards are met with each new release

### New Tests
- Added test to ensure that all ComputeInput classes are data classes 
- ensure that the app loads small(2X CIFAR) data in under ~15 sec
- ensure that the app loads big (3X ImageNet) data in under ~2min
- ensure that after loading 3X Imagenet and then "removing" all tests, they are properly disposed and used memory is less than 500mb (for now)

[//]: # (### Notes)

[//]: # (### Todo)

