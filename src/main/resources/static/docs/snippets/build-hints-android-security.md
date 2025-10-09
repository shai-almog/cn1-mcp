---
topic: build-hints
title: Android integrity and security hints
description: Detect rooted devices, block instrumentation, and tune signing schemes for Android builds.
---
```properties
# Enable Google Play licensing and signing
android.licenseKey=YOUR_GOOGLE_PLAY_LICENSE
android.signingV1=true
android.signingV2=true
android.signingV3=true
android.signingV4=true

# Device integrity checks
android.rootCheck=false
android.fridaDetection=false
android.fridaVersion=1.0.0
android.fridaDebugLogging=false

# Mock location and headphone callbacks
android.mockLocation=true
android.headphoneCallback=false
```
