## Update Instructions:

1. Close the app if it is currently running
2. Download the .dmg file from the latest release
3. Drag the .dmg file to your Applications folder
4. When you try to open the app, you might get an error saying the app is broken. This is because I have not signed up as an "official Apple developer". For now, please type this into the terminal each time you update:

`sudo xattr -cr /Applications/deephy.app`

### New Features

- Added message at bottom if app needs updating
- Loaded new (v2) data files
- Loaded data file from new python template class (`DeephyCborData`)

### Bug Fixes

- Rate limit exceptions from GitHub are better handled
