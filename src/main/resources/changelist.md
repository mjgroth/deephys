<!--- https://github.com/mgroth0/deephy/releases -->

[//]: # (VERSION:1.22.0)


Compatible pip package
version: [0.5.4](https://pypi.org/project/deephys/0.5.4/) ([instructions](https://colab.research.google.com/drive/1HAaVOopHDNVKryP14wW4K_rcqeeqYrLK#scrollTo=VtUgz8xGYKHj))

[//]: # (### PIP Python Package Updated to 0.5.4)

### New Features

- Category View Improvements
  - Removed setting for choosing where top neurons come from in category view
  - instead show top neurons for all 3 choices: ALL (on the bottom), FP, and FN
  - Added text to explain what top neurons on bottom mean
- Tooltip improvements
  - they no longer auto hide. They can be hidden with the escape key
  - they are transparent to mouse events
  - they are moved further away from the mouse
  - They automatically adjust their position if they would be off screen

### Performance Improvements

- Made more javafx properties lazy
- Decreased Xmx to 8GB. Effects for those 8GB RAM will be:
  - generally faster performance as long as less than 8GB is needed from app
  - less RAM usage
  - more GC events. Maybe intermittent slowdowns
  - much greater change of OOM errors. Though, this will help identify memory leaks

[//]: # (### Cosmetic Changes)

### Bug Fixes

- Fixed getWrapper bug for ScrollPane

[//]: # (### Notes)

[//]: # (### Todo)

