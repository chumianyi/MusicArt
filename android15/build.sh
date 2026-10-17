#!/bin/bash
set -e
PROJ=~/MusicArt15
OLDJAR=~/tools/android-1.5_r04-linux/android.jar
J8=~/tools/jdk8u402-b06
AAPT=~/android-sdk/build-tools/34.0.0/aapt
DX=~/tools/android-1.5_r04-linux/tools/lib/dx.jar
KT=~/.android/debug.keystore

cd $PROJ
rm -rf gen obj out && mkdir -p gen obj out

echo "[1/5] aapt (gen R.java + resources)"
$AAPT package -m -J gen -M AndroidManifest.xml -S res -A assets -I $OLDJAR -f

echo "[2/5] javac"
find gen src -name '*.java' > sources.txt
$J8/bin/javac -bootclasspath $OLDJAR -source 1.6 -target 1.6 -encoding UTF-8 \
  -d obj @sources.txt 2>&1 | tee javac.log
grep -v "deprecat" javac.log || true

echo "[3/5] dx (dex 035)"
$J8/bin/java -cp $DX com.android.dx.command.Main --dex --output=obj/classes.dex obj

echo "[4/5] package apk"
$AAPT package -f -M AndroidManifest.xml -S res -A assets -I $OLDJAR -F out/app.unaligned.apk
( cd obj && $AAPT add ../out/app.unaligned.apk classes.dex )

echo "[5/5] sign"
[ -f $KT ] || $J8/bin/keytool -genkey -keystore $KT -alias k -dname "CN=D" \
   -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000
$J8/bin/jarsigner -keystore $KT -storepass android -keypass android \
   -signedjar out/MusicArt15.apk out/app.unaligned.apk k

echo "=== BADGING ==="
$AAPT dump badging out/MusicArt15.apk | grep -E "package:|sdkVersion|launchable"
ls -la out/MusicArt15.apk
