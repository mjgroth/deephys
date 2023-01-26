<!--- https://github.com/mgroth0/deephys/releases -->

[//]: # (VERSION:1.28.0)


Compatible pip package
version: [0.9.0](https://pypi.org/project/deephys/0.9.0/)

### PIP Python Package Updated to 0.9.0
- Added optional `dtype` parameter to `import_test_data`. See the docs.
- `ground_truths` can now be a regular list, a numpy list, or a torch IntTensor
- `pixel_data` can now be `[images,channels,dim1,dim2]` as before or [images,dim1,dim2]` for greyscale
- `state` can now be a `torch.FloatTensor` (previously only numpy and regular list were supported)
- Improved generated documentation by using type hinting


### New Features
- dtype: dtype for activations can now be either float32 or float64
- If the app crashes, a crash report should now be written to a file. This file is located in a different place depending on what OS the user is running. 
- Detailed info about the error logging above added to the readme, as it is critical for us to receive error logs in order to fix most crashes

[//]: # (### Performance Improvements)
[//]: # (### Cosmetic Changes)

### Bug Fixes
- In the "Top Images" calculation for the ByNeuron view, any activations that are NaN or Infinite (as a result of zeros in the activations) are now completely excluded. 
- Checkbox in "bind" tutorial fixed

[//]: # (### Internal Development)
[//]: # (### New Tests)
[//]: # (### Notes)
[//]: # (### Todo)

### User Friendliness
- Added information tooltip for NaN and infinite activation values explaining why they are shown and advising the user to try re-generating the data in float64 format.