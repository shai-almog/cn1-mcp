---
topic: build-hints
title: iOS entitlements and privacy hints
description: Declare iOS capabilities, associated domains, and required privacy usage descriptions.
---
```properties
# Associated domains for universal links and keychain
ios.associatedDomains=applinks:example.com,webcredentials:example.com
ios.app_groups=group.com.example.shared
ios.keychainAccessGroup=com.example.app

# Push notification capability
ios.includePush=true

# URL schemes the app can query
ios.applicationQueriesSchemes=myapp,myotherapp

# Privacy strings required by iOS 10+
ios.locationUsageDescription=We use your location to show nearby offers.
ios.NSCameraUsageDescription=Capture profile photos.
ios.NSMicrophoneUsageDescription=Record voice notes.
ios.NSPhotoLibraryAddUsageDescription=Save edited pictures.
ios.NSPhotoLibraryUsageDescription=Select images to upload.
ios.NSLocationAlwaysUsageDescription=Provide geofenced alerts.
ios.NSSpeechRecognitionUsageDescription=Transcribe voice to text.
ios.NSContactsUsageDescription=Share with friends.
```
