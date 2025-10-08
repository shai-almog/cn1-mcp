# Codename One CSS Overview

Codename One CSS themes map selectors onto UIIDs rather than DOM elements. Use these topic-specific guides for details and ready-to-run snippets:

* [Selectors](Selectors.md)
* [Special Selectors and Constants](SpecialSelectors.md)
* [Core Properties](Properties.md)
* [Backgrounds and Borders](BackgroundsAndBorders.md)
* [Images and Assets](Assets.md)
* [Fonts](Fonts.md)
* [Media Queries](MediaQueries.md)
* [Java Integration](JavaIntegration.md)

## Quick Start Template

Copy the following scaffold into a new `theme.css` and adjust UIIDs as needed:

```css
#Device {
    min-resolution: 120dpi;
    max-resolution: 480dpi;
}

#Constants {
    defaultFontSizeInt: 18;
    defaultDesktopFontSizeInt: 14;
    menuImage: "menu.png";
}

Default {
    font-family: "native:MainRegular";
    color: black;
}

Button {
    padding: 2mm 3mm;
    border: 1pt solid #3399ff;
    cn1-background-type: cn1-image-scaled-fill;
    background: linear-gradient(0deg, #6fa8ff, #2a5cc6);
}

Button.pressed {
    background-color: #204c9b;
}

MyButton {
    cn1-derive: Button;
    background-color: #004b8d;
    text-align: center;
}

@media platform-ios, density-high {
    Button {
        font-size: 2.5mm;
    }
}
```

## Snippet Directory

Practical snippets live in [`css/snippets`](snippets/). Copy the CSS or Java samples directly into MCP prompts when you need starting points.
