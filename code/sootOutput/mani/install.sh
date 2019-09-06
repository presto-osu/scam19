#!/bin/bash

pkg=${1::-4}

echo ..... Uninstall
adb shell pm uninstall $pkg

echo ..... Sign
./sign.sh $1

echo ..... Install
adb install -r $1

