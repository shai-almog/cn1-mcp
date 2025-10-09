---
topic: build-hints
title: Android Play Services and ads hints
description: Select specific Google Play Services artifacts and configure AdMob identifiers for Android builds.
---
```properties
# Legacy toggle (prefer per-service flags below)
android.includeGPlayServices=false

# Enable only the services you need
android.playService.base=true
android.playService.location=true
android.playService.maps=true
android.playService.ads=true
android.playService.plus=false
android.playService.auth=false
android.playService.analytics=false
android.playService.cast=false
android.playService.gcm=false
android.playService.drive=false
android.playService.fitness=false
android.playService.vision=false
android.playService.nearby=false
android.playService.panorama=false
android.playService.games=false
android.playService.safetynet=false
android.playService.wallet=false
android.playService.wearable=false
android.playService.identity=false
android.playService.indexing=false
android.playService.appInvite=false

# Specify a concrete Play Services version (advanced)
android.playServicesVersion=17.0.0

# Provide your AdMob configuration
google.adUnitId=ca-app-pub-XXXXXXXXXXXXXXXX/banner
android.googleAdUnitId=ca-app-pub-XXXXXXXXXXXXXXXX/banner
android.googleAdUnitTestDevice=C6783E2486F0931D9D09FABC65094FDF
```
