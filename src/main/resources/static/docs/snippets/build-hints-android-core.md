---
topic: build-hints
title: Core Android build hints
description: Toggle Android build variants, SDK targeting, ProGuard, multidex, and runtime behaviors.
---
```properties
# Control debug/release artifacts
android.debug=true
android.release=true

# Deployment targeting
android.installLocation=auto
android.min_sdk_version=7
android.targetSDKVersion=21
android.versionCode=1

# Build pipeline controls
android.gradle=true
android.multidex=false
android.enableProguard=true
android.shrinkResources=false
android.proguardKeep=-keep class com.example.MyClass { *; }
android.stack_size=1048576
android.supportV4=false

# Play Services version negotiation
android.playServicesVersion=default
mylib.minPlayServicesVersion=10.0.0

# AndroidX toggle
android.useAndroidX=true

# Lifecycle/behavior flags
android.keyboardOpen=true
android.asyncPaint=true
android.streamMode=music
android.theme=Light
android.statusbar_hidden=false
android.web_loading_hidden=false
android.captureRecord=enabled
android.nonconsumable=premium_upgrade,extra_storage
android.headphoneCallback=false
android.gpsPermission=true
android.mockLocation=true
android.smallScreens=true
android.removeBasePermissions=false
android.blockExternalStoragePermission=false
android.sharedUserId=com.example.shared
android.sharedUserLabel=Shared Account
android.licenseKey=YOUR_GOOGLE_PLAY_LICENSE
```
