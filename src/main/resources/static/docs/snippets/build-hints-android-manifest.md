---
topic: build-hints
title: Android manifest injection hints
description: Inject permissions, attributes, and XML fragments into the generated Android manifest and resources.
---
```properties
# Granular permission control
android.permission.CAMERA=true
android.permission.ACCESS_FINE_LOCATION.required=false
android.permission.RECORD_AUDIO.maxSdkVersion=30

# Bulk permission overrides
android.xpermissions=<uses-permission android:name="android.permission.NFC" />

# Intent filters for main activity
android.xintent_filter=<intent-filter>...</intent-filter>

# Activity launch mode override
android.activity.launchMode=singleTask

# Application and activity attribute injection
android.xapplication=<service android:name="com.example.SyncService" />
android.xapplication_attr=android:usesCleartextTraffic="true"
android.xactivity=android:exported="true"

# Manifest queries for Android 11+
android.manifest.queries=<package android:name="com.example.app" />

# Resource file injections
android.stringsXml=<string name="greeting">Hello</string>
android.style=<item name="android:windowTranslucentStatus">true</item>
android.cusom_layout1=<LinearLayout ... />

# Push and account configuration
android.pushVibratePattern=0,250,100,250
android.facebook_permissions=email,user_friends
```
