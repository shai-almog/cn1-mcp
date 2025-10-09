# Build Hints for Native Interfaces

Codename One "build hints" fine-tune the binaries that the build server emits. They let you
request extra permissions, tweak manifest entries, and inject iOS `Info.plist`
keys without writing auxiliary native build scripts. Native interface projects
rely on build hints to connect Objective-C/Swift, Android, and JavaScript code to
the correct system APIs at runtime.

## Setting Build Hints

* **Codename One Settings UI** – Choose **Codename One → Codename One Settings → Build Hints** to edit
  key/value pairs interactively.
* **`codenameone_settings.properties`** – Add properties that start with the
  `codename1.arg.` prefix. For example, the UI value `android.permission.CAMERA=true`
  becomes `codename1.arg.android.permission.CAMERA=true` when stored directly in
  the properties file.

> **Important:** Build hints only take effect when they are persisted in your
> project's root `codenameone_settings.properties` file (prefixed with
> `codename1.arg.`) or saved through the Codename One Settings UI, which writes
> to the same location.

Codename One applies the hints at build time across the supported targets. The
Android pipeline consumes entries that start with `android.`; iOS reads `ios.`
keys; desktop and JavaScript targets each have their own namespaces.

## Common Native Integrations

```properties
# Request additional Android permissions
android.permission.CAMERA=true
android.permission.ACCESS_FINE_LOCATION.required=false

# Supply iOS privacy descriptions
ios.NSCameraUsageDescription=Capture profile photos inside the app.
ios.NSMicrophoneUsageDescription=Record voice notes for sharing.

# Link extra native libraries
ios.add_libs=libsqlite3.dylib;libz.dylib
android.xapplication=<service android:name="com.example.Service" />
```

Consult the [Codename One developer guide](https://www.codenameone.com/developer-guide.html#_build_hints)
for the continually updated catalogue of options.

## MCP Support for Build Hints

The MCP server includes convenience snippets so you can bootstrap manifests and
privacy descriptions while chatting with the agent. Ask for the `build-hints`
snippet topic to retrieve ready-to-paste key/value pairs grouped by platform:

* **Android** – core pipeline toggles, manifest injections, Play Services and ads,
  push delivery, and device integrity checks.
* **iOS** – build/distribution metadata, privacy entitlements, UI behavior, and
  native code/Info.plist injection samples.
* **JavaScript & Desktop** – proxy settings, HTML head injection, desktop window
  sizing, bundled JVM selection, and macOS/Windows manifest tweaks.
* **Global** – Google Play/AdMob keys, analytics opt-out, and other
  cross-platform flags.

Each snippet file mirrors the keys in the developer guide so you can copy an
entire category directly into `codenameone_settings.properties`.
