---
topic: build-hints
title: Android push and social hints
description: Configure Android push background handling and native Facebook integration credentials.
---
```properties
# Deliver push while app is backgrounded
android.background_push_handling=true

# Google Cloud Messaging / FCM sender id
gcm.sender_id=1234567890

# Facebook native integration
facebook.appId=YOUR_FB_APP_ID
facebook.clientToken=YOUR_FB_CLIENT_TOKEN
android.facebook_permissions=email,user_friends
```
