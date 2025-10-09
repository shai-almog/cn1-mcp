---
topic: build-hints
title: iOS build and distribution hints
description: Configure architectures, distribution methods, rpmalloc usage, and versioning for iOS builds.
---
```properties
# Architectures and bitcode
ios.debug.archs=arm64
ios.release.archs=arm64
ios.bitcode=false

# Distribution channels
ios.distributionMethod=app-store
ios.debug.distributionMethod=ad-hoc
ios.release.distributionMethod=enterprise

# Team identifiers
ios.teamId=TEAMID1234
ios.debug.teamId=DEBUGTEAM123
ios.release.teamId=RELEASETEAM456

# Project flavor and deployment target
ios.project_type=ios
ios.deployment_target=8.0
ios.rpmalloc=true

# Versioning and release aids
ios.bundleVersion=1.0.0
ios.testFlight=false
ios.generateSplashScreens=false

# Xcode version override
ios.xcode_version=9.2
```
