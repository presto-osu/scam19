#!/bin/bash

for name in "$@"; do
  avdmanager delete avd -n "$name"
  # avdmanager -s create avd -n "$name" -k "system-images;android-27;google_apis;x86"
  expect create_avd.expect "$name"
done
