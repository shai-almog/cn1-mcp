# Images and Asset Handling

Codename One CSS can both consume and generate multi-image assets. Use these patterns to control density, locations, and constants.

## Declaring Source DPI

```css
HeroBanner {
    background-image: url(images/banner.png);
    cn1-source-dpi: 320;
}
```

* Values ≤120 are treated as low density, 121–160 as medium, 161–320 as very high, 321–480 as HD, and >480 as 2HD.
* Set `cn1-source-dpi: 0;` to import an image without generating multi-image variants.

## Providing Multi-Image Inputs

Organize pre-rendered densities inside a directory named after the image:

```
css/
 ├── theme.css
 └── images/
      └── logo.png/
           ├── verylow.png
           ├── low.png
           ├── medium.png
           ├── high.png
           ├── veryhigh.png
           ├── hd.png
           └── 2hd.png
```

Then reference the logical name:

```css
BrandHeader {
    background-image: url(images/logo.png);
}
```

## Loading Remote Assets

Remote URLs download once during compilation and are cached locally afterward.

```css
Splash {
    background-image: url(https://example.com/art/splash.png);
}
```

## Image Constants

Strings ending with `Image` inside `#Constants` resolve to multi-images if the asset exists:

```css
#Constants {
    menuImage: "menu.png";
}
```

This example searches `res/theme.css/menu.png/` for the density variants. If they are missing, ensure the image appears in a `background-image` elsewhere in the CSS.

## Importing Bundles of Images

Declare an unused selector to pull multiple assets into the compiled resource file:

```css
Images {
    background-image: url(images/NowLogo.png),
        url(images/Username-icon.png),
        url(images/Password-icon.png);
}
```

At runtime, use `Resources` to retrieve them:

```java
Resources theme = Resources.openLayered("/theme");
Label bookmark = new Label(theme.getImage("Password-icon.png"));
```
