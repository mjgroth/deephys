<!--- https://github.com/mgroth0/deephy -->

## Update Instructions:

1. Close the app if it is currently running
2. Download the .zip file from the latest [release](https://github.com/mgroth0/deephy/releases)
3. Open the .zip
4. Drag the .app file to your Applications folder and overwrite if prompted

## Testing Instructions

1. Download the latest [release](https://github.com/mgroth0/deephy/releases)
2. Download or generate data
    - To download, get the latest [data files](https://drive.google.com/drive/folders/1cV8k84p0_kC5l0KfFhHPYjJNwKxVDYa6)
      from Google Drive
        - `insert_model_name_here_anirban.model`
        - `CIFARV1_test.test`
        - `CIFARV2.test`
    - To generate, follow the instructions for the compatible pip package version. Make sure to follow the instructions
      for the correct version, as writen in the [release notes](https://github.com/mgroth0/deephy/releases) for the
      version you are using
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
    6. Click on a neuron name to go back to seeing top images for that neuron, or click another image to navigate to the
       top neurons for that image

## Intel Mac Instructions

1. Download the `deephys-mac-intel.zip` for a release
2. Unzip it
3. From terminal, execute the file `intel` inside of it
    - Note: it will install homebrew and openjdk17 if you do not already have it. This may take a while but will only
      happen the first time you open the app.

## Running From Source

1. `mkdir "deephys"`
2. `cd "deephys"`
3. `mkdir "k"`
4. `cd k`
5. `mkdir nn`
6. `cd nn`
7. `echo "{}" > build.json` 
1. `git clone https://github.com/mgroth0/deephys`
2. `cd ..`
2. `git clone https://github.com/mgroth0/math`
3. `git clone https://github.com/mgroth0/lang`
4. `git clone https://github.com/mgroth0/prim`
5. `git clone https://github.com/mgroth0/collect`
6. `git clone https://github.com/mgroth0/file`
7. `git clone https://github.com/mgroth0/gui`
8. `git clone https://github.com/mgroth0/model`
9. `git clone https://github.com/mgroth0/fx`
10. `git clone https://github.com/mgroth0/obs`
11. `git clone https://github.com/mgroth0/log`
12. `git clone https://github.com/mgroth0/caching`
13. `git clone https://github.com/mgroth0/kjlib`
14. `git clone https://github.com/mgroth0/pref`
15. `git clone https://github.com/mgroth0/test`
16. `git clone https://github.com/mgroth0/cbor`
17. `git clone https://github.com/mgroth0/async`
18. `git clone https://github.com/mgroth0/reflect`
19. `git clone https://github.com/mgroth0/stream`
20. `git clone https://github.com/mgroth0/time`
21. `git clone https://github.com/mgroth0/exec`
22. `git clone https://github.com/mgroth0/json`
23. `git clone https://github.com/mgroth0/service`
24. `git clone https://github.com/mgroth0/http`
25. `git clone https://github.com/mgroth0/html`
26. `git clone https://github.com/mgroth0/sys`
27. `git clone --recurse-submodules https://github.com/mgroth0/hurricanefx`
28. `git clone https://github.com/mgroth0/css`
29. `git clone https://github.com/mgroth0/hotkey`
30. `git clone https://github.com/mgroth0/color`
31. `git clone https://github.com/mgroth0/mstruct`
32. `git clone https://github.com/mgroth0/remote`
33. `git clone https://github.com/mgroth0/auto`
34. `git clone https://github.com/mgroth0/key`
35. `git clone https://github.com/mgroth0/conda`
2. `cp nn/deephys/gradlew ..`
3. `cp -r nn/deephys/gradle ..`
2. `cd ..`
3. Install `brew` (only if you don't already have it)
    4. `/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"`
    5. Follow any additional steps that Homebrew installation gives you in order to add `brew` command to shell path
5. `brew install openjdk@17`
8. `curl https://gradle.nyc3.digitaloceanspaces.com//kbuild.zip --output kbuild.zip`
9. `curl https://gradle.nyc3.digitaloceanspaces.com//settings.gradle.kts --output settings.gradle.kts`
9. `unzip kbuild.zip`
2. `chmod +x gradlew`
3. `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :k:nn:deephys:run --stacktrace -Dorg.gradle.jvmargs="-Xms4g -Xmx4g"`