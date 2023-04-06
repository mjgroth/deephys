[![pip](https://img.shields.io/badge/compatible%20pip%20version-0.14.0-00bbe2?logo=pypi&logoColor=f5c39e)](https://pypi.org/project/deephys/0.14.0)




### Performance Improvements
- Might improved build performance and reduced build file sizes by remving unneccesary JVM modules 


### User Friendliness
- Added exe icon for windows ARM version






### Bug Fixes
- Fixed a bug caused by a library that included signed jars. "unsigned" the jars.
- "Send Feedback" button works in a new thread to avoid crashes
- Prevent clicking "send feedback" button twice
- Did the same two above things with the "report bug" and "view bug" button
- Set default exception handler for threads created before GUI started (helps error handling, less silent errors and crashes, more logging)







