<!--- https://github.com/mgroth0/deephy/releases -->

[//]: # (VERSION:1.18.0)


Compatible pip package
version: [0.5.3](https://pypi.org/project/deephy/0.5.3/) ([instructions](https://colab.research.google.com/drive/1HAaVOopHDNVKryP14wW4K_rcqeeqYrLK#scrollTo=VtUgz8xGYKHj))

[//]: # (### PIP Python Package Updated to 0.5.0)

### New Features
- Added progress bar for test image loading
- Added tooltips with full category labels to pie charts
- Added tooltips to all images with ground truth labels 

### Performance Improvements
- Custom-written matt.math.argmaxn algorithm should compute top-k images 10 times faster.  
- A lot of small and large optimizations to make the app start much faster
- Preload activation matrix so first time seeing ByNeuron view is faster
- Dramatically improved performance of ByCategory view by being more careful to only compute what is necessary (previously sorted all category predictions for all images when really only the top-1 prediction was needed and optimization with matrix math was possible) 

### Cosmetic Changes
- Changed font and size of test viewer titles
- Truncated labels in Category Pie Charts


### Bug Fixes
- Fixed bug caused by trying to get sigfigs of NaN or infinite values (this is probably what caused app to crash when trying to normalize values)
- Fixed NPE caused by backup folder not existing
- Fixed bug causing settings to never save

[//]: # (### Notes)

[//]: # (### Todo)

