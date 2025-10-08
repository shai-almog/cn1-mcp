# Media Queries

Codename One’s CSS compiler evaluates media queries during build time to emit platform- and density-specific styles.

## Supported Tokens

* `platform-xxx` – e.g. `platform-and`, `platform-ios`, `platform-mac`, `platform-win`
* `density-xxx` – e.g. `density-low`, `density-high`, `density-2hd`
* `device-xxx` – `device-phone`, `device-tablet`, `device-desktop`

## Example: Platform Overrides

```css
Label {
    color: black;
}

@media platform-and {
    Label { color: green; }
}

@media platform-ios {
    Label { color: red; }
}
```

Styles inside `@media` blocks override declarations outside them.

## Example: Density Buckets

```css
@media density-very-low, density-low, density-medium {
    Label { font-size: 2.2mm; }
}

@media density-high, density-very-high, density-hd, density-2hd {
    Label { font-size: 2.6mm; }
}
```

Same-type queries separated by commas are OR’d together; different types are AND’d.

## Example: Compound Queries

```css
@media platform-and, density-high {
    Button { font-size: 2.4mm; }
}

@media platform-ios, density-high, density-low {
    Button { font-size: 2.5mm; }
}
```

When multiple media blocks target the same selector, precedence is determined by the number of matched tokens, then by token type order (platform > device > density).

## Font Scaling Constants

Instead of redefining every selector, apply font scaling through constants:

```css
#Constants {
    device-phone-font-scale: "1.5";
    platform-ios-font-scale: "0.9";
    density-low-font-scale: "1.2";
}
```

All matching scale values multiply together at runtime.
