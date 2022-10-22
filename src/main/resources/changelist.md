<!--- https://github.com/mgroth0/deephys/releases -->

[//]: # (VERSION:1.22.2)


Compatible pip package
version: [0.6.0](https://pypi.org/project/deephys/0.6.0/) ([instructions](https://colab.research.google.com/drive/1aR5lnpVMxda7wUj1RZ6YODX5N2FA8YRn))

[//]: # (### PIP Python Package Updated to 0.6.0)

[//]: # (### New Features)

[//]: # (### Performance Improvements)

[//]: # (### Cosmetic Changes)

### Bug Fixes

- Temporary workaround to prevent duplicate node in AnchorPane bug (will need to find real cause and fix it later)
- Fixed deep issues in observable value library which should fix or prevent various issues and possibly improve performance:
  1. `NewAndOldListener` giving wrong "old" values 
  2. `MyBinding` classes were deadlocking)
  3. Similarly, `MObservableImpl` was deadlocking when listener list was modified while updating. Implemented `AntiDeadlockSynchronizer` with good logic to fix and optimize this.

[//]: # (### Notes)

[//]: # (### Todo)

