# Fonts

Codename One CSS supports native fonts, @font-face imports, and Codename One theme constants for default sizing.

## Native Fonts

```css
SideCommand {
    font-family: "native:MainThin";
    font-size: 1.8mm;
}
```

Available families include `native:MainThin`, `native:MainLight`, `native:MainRegular`, `native:MainBold`, `native:MainBlack`, and their italic counterparts.

## TrueType / OpenType Fonts

Define fonts with `@font-face` and reference them by the declared family name:

```css
@font-face {
    font-family: "Montserrat";
    src: url(res/Montserrat-Regular.ttf);
}

Heading {
    font-family: "Montserrat";
    font-size: 4mm;
}
```

Remote URLs are downloaded once during compilation and cached locally.

## Font Sizes

Prefer physical units such as millimetres (`mm`) for consistent results across densities.

```css
Label {
    font-size: 2.2mm;
}
```

Percentages are measured relative to the platform’s medium font size (`150%` == `1.5rem`).

## Theme-Level Defaults

Set baseline font sizes with constants so your design scales predictably.

```css
#Constants {
    defaultFontSizeInt: 18;
    defaultDesktopFontSizeInt: 14;
    device-phone-font-scale: "1.2";
}
```

`device-*`, `platform-*`, and `density-*` font-scale constants multiply together when multiple entries apply at runtime.
