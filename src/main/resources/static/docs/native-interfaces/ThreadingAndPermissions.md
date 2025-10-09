# Threading, Permissions, and Assets

## Threading Guidelines

* **Android** – Use `AndroidNativeUtil.getActivity().runOnUiThread(...)` for asynchronous UI work or `AndroidImplementation.runOnUiThreadAndBlock(...)` when you need to block the Codename One EDT.
* **iOS** – Wrap UI calls with `dispatch_async(dispatch_get_main_queue(), ^{ … })` (or `dispatch_sync` for blocking scenarios, being mindful of deadlocks).
* **JavaScript** – Always reply through the provided callback. Omitting `callback.complete()` or `callback.error()` will deadlock the calling Java thread.

## Permissions, Assets, and Build Hints

Native Android code may need additional manifest permissions. Declare them with build hints such as:

```
android.permission.CAMERA=true
android.permission.ACCESS_FINE_LOCATION.required=false
android.permission.READ_CALENDAR.maxSdkVersion=28
```

Alternatively inject raw XML using `android.xpermissions=<uses-permission android:name="android.permission.READ_CALENDAR" />`.
See [Build Hints for Native Interfaces](BuildHints.md) for additional targets and formatting
guidelines.

Native libraries (`.jar`, `.a`, etc.) should be placed inside the relevant `native/<platform>` folder so the build server packages them automatically.
