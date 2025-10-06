# Codename One Restrictions (Strict)

## Language level
- Java 8 syntax only (lambdas, method refs, default methods OK). Streams are not supported. No records, var or API beyond what compiles to Java 8 bytecode.

## UI
- Use `com.codename1.ui.*` only. **Never** use AWT/Swing/JavaFX.
- Layouts: `BorderLayout`, `BoxLayout`, `FlowLayout`, etc., from CN1.

## EDT (Event Dispatch Thread)
- All UI mutations must run on the EDT.
- Wrap with `Display.getInstance().callSerially(...)` (or `callSeriallyAndWait(...)` when needed).
- Never block the EDT (no `Thread.sleep(...)`, no synchronous I/O).

## Networking
- Use `ConnectionRequest` or `Rest` API with callbacks/futures.
- No direct `HttpURLConnection` or blocking I/O on the EDT.

## Storage & Files
- Use `Storage`, `FileSystemStorage`, `Preferences`.
- No absolute desktop paths like `C:\...`, `/Users/...`, `/home/...`, `/tmp/...`.

## Threads & Background Work
- Prefer `CN.execute(...)`, `NetworkManager`, `UITimer`.
- Avoid raw `new Thread(...)` for app logic; never block EDT.

## Reflection & Native Access
- Reflection isn't supported; avoid `java.lang.reflect.*` patterns.
- Don't rely on class names as code is obfuscated, avoid `getClass().getName()`
- Use CN1 native interfaces (`cn1lib`) instead of platform APIs directly.

## Forbidden APIs (strict errors)
- `java.awt.*`, `javax.swing.*`, `javafx.*`
- `java.nio.file.*`
- `java.lang.Process*`, `Runtime.getRuntime().exec(...)`
- `java.lang.reflect.*`
- `java.sql.*`
- `java.net.HttpURLConnection`

## Theming
- Prefer UIIDs + CN1 CSS/Theme. Minimize per-component, hard-coded styles.

## Lifecycle
- Respect `init/start/stop/destroy` flow. Show forms from `start()` or on the EDT.