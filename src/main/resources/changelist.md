<!--- https://github.com/mgroth0/deephy/releases -->

[//]: # (VERSION:1.12.0)


Compatible pip package
version: [0.4.0](https://pypi.org/project/deephy/0.4.0/) ([instructions](https://colab.research.google.com/drive/1PNiGD26uBsktq64fqPg76yoN-ruixavj))


### New Features
- If ", texture :" is detected in category name, only the text found be fore the comma is displayed in the predictions list

### Performance Improvements

- Set up a new asynchronous loading system, improving performance throughout app
- Reduced RAM usage by using floats instead of doubles
- Implemented Multik to get maximum neuron activations must faster


### Cosmetic Changes
- Greatly improved the look of the text and spacing in the ByImage view. Some of the information is now truncated or rounded again, but it can all be seen by mousing over the text and looking at the tooltip

### Bug Fixes
- top neuron sorting now properly follows the normalization setting

[//]: # (### Notes)

[//]: # (### Todo)

