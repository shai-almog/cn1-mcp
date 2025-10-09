# Callbacks from Native Code

## Java-Based Platforms

Android, Java SE, RIM, and Java ME use Java for the native layer. Call static Java callbacks directly:

```java
com.mycompany.NativeCallback.callback();
com.mycompany.NativeCallback.callback("My Arg");
```

## Objective-C (iOS)

Include the generated headers and call the mangled method name. The `CodenameOne_GLViewController.h` header defines macros for threading state.

```objectivec
#include "com_mycompany_NativeCallback.h"
#include "CodenameOne_GLViewController.h"

com_mycompany_NativeCallback_callback__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
com_mycompany_NativeCallback_callback___int(CN1_THREAD_GET_STATE_PASS_ARG intValue);
com_mycompany_NativeCallback_callback___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG nsStringValue));
```

To return values, note the `_R_` suffix that encodes the return type:

```objectivec
com_mycompany_NativeCallback_callback___int_R_int(intValue);
```

## JavaScript

The `$GLOBAL$` bridge exposes static Java methods to native JavaScript. Append `$async` when the target method uses Codename One threading primitives.

```javascript
this.$GLOBAL$.com_codename1_googlemaps_MapContainer.fireMapChangeEvent__int_int_double_double$async(mapId, zoom, lat, lon);
```

Your code **must** include the literal string `this.$GLOBAL$.package_Class.method` so the build server can detect and expose the callback.
