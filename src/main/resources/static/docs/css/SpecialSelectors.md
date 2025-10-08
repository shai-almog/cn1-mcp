# Special Selectors and Theme Constants

Codename One includes three special selectors that influence build-time behavior.

## `#Device`

Restrict the densities generated when compiling CSS into a resource file.

```css
#Device {
    min-resolution: 160dpi;
    max-resolution: 480dpi;
    resolution: 320dpi; /* optional default */
}
```

Only devices inside the inclusive range receive tailored multi-image assets.

## `#Constants`

Declare theme constants, image bindings, and CSS variables.

```css
#Constants {
    PopupDialogArrowBool: false;
    sideMenuImage: "menu.png"; /* resolves in res/theme.css/menu.png/ */
    --main-bg: #ececec;        /* CSS variable */
}
```

* String values ending with `Image` create multi-image constants if an asset exists at `res/<cssfile>/<name>/` or as a background image elsewhere in the stylesheet.
* Use constants for booleans, numbers, enums, and fallback font scaling (e.g. `defaultFontSizeInt`).

## `Default`

Set properties on the implicit base UIID that all other UIIDs derive from:

```css
Default {
    font-family: "native:MainLight";
    color: #202020;
}
```

Define cross-theme defaults (fonts, colors) here instead of repeating them in each UIID.
