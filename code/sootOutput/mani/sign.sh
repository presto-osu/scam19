#!/bin/sh

apkPath=$1
# password: wy092883
# zip -d $apkPath META-INF/\*
# jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore my-release-key.keystore $apkPath  alias_name

zip -d $apkPath META-INF/\*
expect ./sign.expect $apkPath ./my-release-key.keystore

echo ..... Zipalign
mv $apkPath .$apkPath
zipalign -v 4 .$apkPath $apkPath
zipalign -c -v 4 $apkPath
rm .$apkPath

