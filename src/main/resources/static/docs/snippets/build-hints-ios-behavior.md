---
topic: build-hints
title: iOS UI and runtime behavior hints
description: Control keyboard handling, multitasking, theming, and runtime safeguards on iOS.
---
```properties
# Keyboard and application lifecycle
ios.keyboardOpen=true
ios.application_exits=false

# Status bar and theme
ios.statusbar_hidden=false
ios.themeMode=default

# Orientation and multitasking
ios.interface_orientation=UIInterfaceOrientationPortrait:UIInterfaceOrientationLandscapeLeft
ios.multitasking=true

# Rendering pipelines and storage
ios.newPipeline=true
ios.newStorageLocation=true

# Visual tweaks
ios.prerendered_icon=false
ios.enableBadgeClear=true

# Media behavior
ios.useAVKit=false
ios.enableAutoplayVideo=false

# Security options
ios.detectJailbreak=false
ios.blockScreenshotsOnEnterBackground=false

ios.headphoneCallback=false
```
