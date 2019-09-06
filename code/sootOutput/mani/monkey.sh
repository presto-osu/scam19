#!/bin/bash

# $1 apk
# $2 throttle
# $3 num of events

# hide notification bar and virtual keyboard
# adb shell settings put global policy_control immersive.full=*

# pkg=${1::-4}
# adb shell monkey -p $pkg \
#   --pct-appswitch 0 --pct-trackball 0 --pct-syskeys 0 \
#   --throttle $2 -vvv $3

# restore
# adb shell settings put global policy_control null

python3 run.py ${@:1}
