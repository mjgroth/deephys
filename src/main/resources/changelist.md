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

[//]: # (### Cosmetic Changes)

### Bug Fixes
- Fixed a "file load stream broken" bug 

[//]: # (### Internal Development)
[//]: # (### New Tests)
[//]: # (### Notes)
[//]: # (### Todo)