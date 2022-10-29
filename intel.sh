#!/usr/bin/env bash

# https://stackoverflow.com/questions/59895/how-do-i-get-the-directory-where-a-bash-script-is-located-from-within-the-script
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# https://stackoverflow.com/questions/21577968/how-to-tell-if-homebrew-is-installed-on-mac-os-x
which -s brew
if [[ $? != 0 ]] ; then
    # Install Homebrew
    ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
else
    echo "brew exists."
#    brew update
fi

# https://linuxize.com/post/bash-check-if-file-exists/
JAV="/usr/local/Cellar/openjdk@17/17.0.5/bin/java"
if [ -f "$JAV" ]; then
    echo "$FILE exists."
else
    echo "$FILE does not exist."
    brew install openjdk@17
fi

/usr/local/Cellar/openjdk@17/17.0.5/bin/java