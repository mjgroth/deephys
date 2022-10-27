<!--- https://github.com/mgroth0/deephys/releases -->

[//]: # (VERSION:1.22.2)


Compatible pip package
version: [0.6.0](https://pypi.org/project/deephys/0.6.0/) ([instructions](https://colab.research.google.com/drive/1aR5lnpVMxda7wUj1RZ6YODX5N2FA8YRn))

[//]: # (### PIP Python Package Updated to 0.6.0)

[//]: # (### New Features)

### Performance Improvements
- Changed some activations properties from soft to weak to reduce memory usage (and hopefully avoid OOM error)
- Removed unnecessary pre-loading code that was optimized for a machine with tons of ram but hurt machines with much less ram
- nullified objects

### Cosmetic Changes

### Bug Fixes

- Temporary workaround to prevent duplicate node in AnchorPane bug (will need to find real cause and fix it later)

- Fixed deep issues in observable value library which should fix or prevent various issues and possibly improve

  performance:

    1. `NewAndOldListener` giving wrong "old" values

    2. `MyBinding` classes were deadlocking)

    3. Similarly, `MObservableImpl` was deadlocking when listener list was modified while updating.

       Implemented `AntiDeadlockSynchronizer` with good logic to fix and optimize this. 

- Fixed background color of "bind" button not changing when selected and replaced it with gradient
- Added scroll bar to root node so there is no more vertical constraints

### Internal Development
- Added ability to print threads to console from settings

[//]: # (### Notes)

[//]: # (### Todo)

