<!--- https://github.com/mgroth0/deephys/releases -->

[//]: # (VERSION:1.22.7)


Compatible pip package
version: [0.6.0](https://pypi.org/project/deephys/0.6.0/) ([instructions](https://colab.research.google.com/drive/1aR5lnpVMxda7wUj1RZ6YODX5N2FA8YRn))

[//]: # (### PIP Python Package Updated to 0.6.0)
### New Features
- Loading bar pop up for top images (may not be needed in the end due to performance improvements below)

### Performance Improvements
- reduced loading time and deadlocks by optimizing calculations
- Fixed memory leaks when removing tests
- No more "spinning beach ball" loading

[//]: # (### Cosmetic Changes)

### Bug Fixes
- Tests are correctly disposed from memory even after entering ByCategoryView
- When bound dataset is un-bound, toggle correctly shows as being deselected
- PieChart labels show in both light and dark mode now (previously didn't work in light mode)

### Internal Development
- New tests for category view
- Misc. Fixes


[//]: # (### New Tests)
[//]: # (### Notes)
[//]: # (### Todo)

