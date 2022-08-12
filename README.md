## Update Instructions:

1. Close the app if it is currently running
2. Download the .zip file from the latest [release](https://github.com/mgroth0/deephy/releases)
3. Open the .zip
4. Drag the .app file to your Applications folder and overwrite if prompted

## Testing Instructions
1. Download the latest version (1.7.0) from https://github.com/mgroth0/deephy/releases
2. Download the latest data file (CIFARV1_test.cbor) made at https://drive.google.com/file/d/1-2yPMQG5OjiQyuLgAG_s4rF53jlcPc2m/view?usp=sharing
3. Feel free to move the data file and the app from your Downloads folder into any other folder. They do not have to be together.
4. Navigate in terminal to the folder `deephy.app/Contents/MacOS`
5. run the command `./deephy`
   - Note: normally you can execute the app just by double clicking the `.app` file, but by executing it through Terminal you will be able to see any error messages.
6. In the app, choose the data folder that contains CIFARV1_test.cbor 
7. Click “load data”. It should take a few seconds to load.
8. Choose a neuron. You should see its top images.

Let me know whether it works or not. If it does not work as expected, please share any error messages that show in Terminal.