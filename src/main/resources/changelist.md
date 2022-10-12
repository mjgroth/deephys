<!--- https://github.com/mgroth0/deephy/releases -->

[//]: # (VERSION:1.22.0)


Compatible pip package
version: [0.5.4](https://pypi.org/project/deephys/0.5.4/) ([instructions](https://colab.research.google.com/drive/1HAaVOopHDNVKryP14wW4K_rcqeeqYrLK#scrollTo=VtUgz8xGYKHj))

### PIP Python Package Updated to 0.6.0
- Added `Features` property to ImageFile

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
- Decreased Xmx to 6GB. Effects for those 6GB RAM will be:
    - generally faster performance as long as less than 8GB is needed from app
    - less RAM usage
    - more GC events. Maybe intermittent slowdowns
    - much greater chance of OOM errors. Though, this will help identify memory leaks
- Started addressed memory issues (decreasing RAM usage and heap reference map complexity) by:
    - Converting properties from a strong to a weak/soft reference
    - disposing of properties manually when done
    - Reduced number of stored objects/properties
    - More lazy properties
    - etc
- Implemented a new image and activation data caching system, alongside renewable weak references to the cached pixel
  and activation values
    - The second progress bar indicates the progress of caching the pixel data for the dataset
- Throttle test loader to help prevent OOM errors

[//]: # (### Cosmetic Changes)

### Bug Fixes

- Fixed getWrapper bug for ScrollPane
- Changed text nodes to labels (which auto-truncate to fit space) in category view to avoid layout issues when category
  label is huge
- Take at most 25 categories for the category pie chart so in a case where there are many categories that are confused
  the pie chart isn't overwhelmed

[//]: # (### Notes)

[//]: # (### Todo)

