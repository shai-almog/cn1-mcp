---
topic: build-hints
title: iOS native code injection hints
description: Inject Info.plist keys, entitlements, and Objective-C snippets during the iOS build.
---
```properties
# Info.plist customization
ios.plistInject=<key>NSCameraUsageDescription</key><string>Capture photos</string>
ios.urlScheme=<string>myapp</string>

# Entitlements
ios.entitlementsInject=<key>aps-environment</key><string>production</string>

# Objective-C code hooks
ios.glAppDelegateHeader=#import "MyManager.h"
ios.glAppDelegateBody=[[MyManager shared] configure];
ios.beforeFinishLaunching=[MyManager setupBeforeLaunch];
ios.afterFinishLaunching=[MyManager setupAfterLaunch];
ios.applicationDidEnterBackground=[MyManager pause];
ios.viewDidLoad=[self configureLayout];

# Libraries and pods
ios.add_libs=libsqlite3.dylib;libz.dylib
ios.pods=AFNetworking ~> 2.6, SwiftyJSON ~> 2.3
ios.pods.platform=9.0
ios.objC=true

# Advertising
ios.googleAdUnitId=ca-app-pub-XXXXXXXXXXXXXXXX/banner
ios.googleAdUnitIdPadding=16

ios.facebook_permissions=email,user_friends
```
