mkdir deephys
cd deephys
mkdir k
cd k
mkdir nn
cd nn
echo {} > build.json
git clone https://github.com/mgroth0/deephys
cd ..
git clone https://github.com/mgroth0/math
git clone https://github.com/mgroth0/lang
git clone https://github.com/mgroth0/prim
git clone https://github.com/mgroth0/collect
git clone https://github.com/mgroth0/file
git clone https://github.com/mgroth0/gui
git clone https://github.com/mgroth0/model
git clone https://github.com/mgroth0/fx
git clone https://github.com/mgroth0/obs
git clone https://github.com/mgroth0/log
git clone https://github.com/mgroth0/caching
git clone https://github.com/mgroth0/kjlib
git clone https://github.com/mgroth0/pref
git clone https://github.com/mgroth0/test
git clone https://github.com/mgroth0/cbor
git clone https://github.com/mgroth0/async
git clone https://github.com/mgroth0/reflect
git clone https://github.com/mgroth0/stream
git clone https://github.com/mgroth0/time
git clone https://github.com/mgroth0/exec
git clone https://github.com/mgroth0/json
git clone https://github.com/mgroth0/service
git clone https://github.com/mgroth0/http
git clone https://github.com/mgroth0/html
git clone https://github.com/mgroth0/sys
git clone https://github.com/mgroth0/css
git clone https://github.com/mgroth0/hotkey
git clone https://github.com/mgroth0/color
git clone https://github.com/mgroth0/mstruct
git clone https://github.com/mgroth0/remote
git clone https://github.com/mgroth0/auto
git clone https://github.com/mgroth0/key
git clone https://github.com/mgroth0/conda
git clone --recurse-submodules https://github.com/mgroth0/hurricanefx
cp nn/deephys/gradlew ..
cp -r nn/deephys/gradle ..
cd ..
# https://linuxize.com/post/bash-check-if-file-exists/
JAV="/opt/homebrew/opt/openjdk@17"
if [ -f "$JAV" ]; then
	echo "$FILE exists."
else
	echo "$FILE does not exist. Please install openjdk@17 with brew"
fi
curl https://gradle.nyc3.digitaloceanspaces.com//kbuild.zip --output kbuild.zip
curl https://gradle.nyc3.digitaloceanspaces.com//settings.gradle.kts --output settings.gradle.kts
unzip kbuild.zip
chmod +x gradlew