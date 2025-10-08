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

## CSS Support (Codename One Theme Compiler)
- See the `css/` subdirectory in this folder for topic-specific guides and ready-to-copy snippets. Start with `css/Overview.md`.
- Selectors map to UIIDs, with `.pressed`, `.selected`, `.unselected`, and `.disabled` as the built-in pseudo classes for component state. Use comma lists for multiple UIIDs and `cn1-derive` for inheritance.
- `#Device`, `#Constants`, and `Default` selectors respectively constrain generated resolutions, declare theme constants (including multi-images), and populate the implicit base UIID.
- Standard properties include padding/margin, border (and variants), border-radius, background/background-image/background-repeat, border-image/border-image-slice, font*, color, text-align, text-decoration (including `cn1-3d` variants), opacity, box-shadow, and width/height (for asset generation).
- Codename One extensions cover `cn1-source-dpi`, `cn1-background-type`, `cn1-derive`, `cn1-9patch` (deprecated), and the `cn1-box-shadow-*` family for round/pill borders.
- CSS variables via `var(--name, fallback)` are supported inside property values; define them in `#Constants` or selectors.
- Complex borders/backgrounds fall back to 9-piece images when they can’t be rendered natively; use `cn1-round-border`, `cn1-pill-border`, or simplified gradients/shadows to avoid bloating the resource file.
- Background precedence: image borders override background images, which override colors. Use `border: none` to reveal lower-priority backgrounds.
- Images can be batched via comma-separated `background-image` declarations. Remote URLs are allowed for single assets (including fonts) but multi-image inputs must be local and organized by density (`verylow.png`, `low.png`, … `4k.png`).
- Fonts default to `native:*` families. Bundle TTF/OTF fonts with `@font-face` (local, HTTP(S), or `github://` sources) and normalize base sizes with constants like `defaultFontSizeInt` and `defaultDesktopFontSizeInt`.
- Media queries accept only `platform-*`, `device-*`, and `density-*`. Terms separated by commas combine (`OR` within type, `AND` across types). Precedence favors media-query styles, then more-specific query groups, with tie-breaker order platform > device > density. Matching font-scale constants (e.g., `device-desktop-font-scale`) multiply together.

## Lifecycle
- Respect `init/start/stop/destroy` flow. Show forms from `start()` or on the EDT.

