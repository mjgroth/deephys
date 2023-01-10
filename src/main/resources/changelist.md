<!--- https://github.com/mgroth0/deephys/releases -->

[//]: # (VERSION:1.25.0)


Compatible pip package
version: [0.7.2](https://pypi.org/project/deephys/0.7.2/)

[//]: # (### PIP Python Package Updated to 0.7.2)

### New Features
- Added setting to control how long tooltips show (0 means infinite. if you don't like this, I suggest seeing how "1000" feels)
- integer settings can be set by typing
- Added percentages to the list view of confusion categories

[//]: # (### Performance Improvements)

### Cosmetic Changes
- Increased size of settings window and settings spinners
- Hide activation text for top neurons when it is not based on image(s) shown (this information is still available in the neuron view) 

### Bug Fixes

- Fixed cosmetic bug where it looked like 2 datasets were "bound" at the same time
- Fixed a bug causing tooltips to not always show

[//]: # (### Internal Development)

[//]: # (### New Tests)

### Notes
- Expanded API [documentation](https://matt-groth-deephys.readthedocs-hosted.com/en/latest/api.html) for `deephys.deephys.Model`
- Fixed broken link in documentation

[//]: # (### Todo)