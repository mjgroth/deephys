<!--- https://github.com/mgroth0/deephy/releases -->

[//]: # (VERSION:1.15.0)


Compatible pip package
version: [0.5.0](https://pypi.org/project/deephy/0.4.0/) ([instructions](https://colab.research.google.com/drive/1PNiGD26uBsktq64fqPg76yoN-ruixavj))

[//]: # (### Python)

### New Features
- Added convenience function `import_torch_dataset` to pip package (see instructions above)
- Creating an `ImageFile` directly now will be more difficult. If you cannot migrate to `import_torch_dataset`, let me know

### Performance Improvements

- Changed pixel format from float64 to int8
- Changed activation format from float64 to float32
- Updated python package to save with these new formats
- Saving `Test`data from using the pip package should be 5-6 times faster
- `.test` files should be 7-8 times smaller
- Loading tests in app should be faster
- Clicking images in app should be faster



[//]: # (### Cosmetic Changes)

[//]: # (### Bug Fixes)


[//]: # (### Notes)

[//]: # (### Todo)

