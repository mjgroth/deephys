<!--- https://github.com/mgroth0/deephy -->

## Update Instructions:

1. Close the app if it is currently running
2. Download the .zip file from the latest [release](https://github.com/mgroth0/deephy/releases)
3. Open the .zip
4. Drag the .app file to your Applications folder and overwrite if prompted

## Testing Instructions

1. Download the latest [release](https://github.com/mgroth0/deephy/releases)
2. Download or generate data
   - To download, get the latest [data files](https://drive.google.com/drive/folders/1cV8k84p0_kC5l0KfFhHPYjJNwKxVDYa6) from Google Drive
        - `insert_model_name_here_anirban.model` 
        - `CIFARV1_test.test`
        - `CIFARV2.test`
   - To generate, follow the instructions for the compatible pip package version. Make sure to follow the instructions for the correct version, as writen in the [release notes](https://github.com/mgroth0/deephy/releases) for the version you are using
3. Run the app
   - Normally, the app can be run by double clicking the .app
   - If there are any bugs, run the app from the terminal so that you can see error messages.
     1. Navigate in terminal to the folder `deephy.app/Contents/MacOS`
     2. Run the command `./deephy`
     3. If you see any error messages in the terminal, please copy them and send them to me.
4. Test different features of the app
   1. Load the `.model` file into the app
   2. Load both `.test` files into the app
   3. Select a layer and neuron to view top images
   4. Press "Bind" so the top and bottom match eachother
   5. Click on an image to see top neurons
   6. Click on a neuron name to go back to seeing top images for that neuron, or click another image to navigate to the top neurons for that image

## Intel Mac Instructions
1. Download the `deephys-mac-intel.zip` for a release
2. Unzip it
3. From terminal, execute the file `intel` inside of it
    - Note: it will install homebrew and openjdk17 if you do not already have it. This may take a while but will only happen the first time you open the app.