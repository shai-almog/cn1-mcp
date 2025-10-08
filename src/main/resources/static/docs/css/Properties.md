# Core Properties

Codename One supports a useful subset of standard CSS properties. This sheet highlights the most common ones with sample usage.

## Spacing

```css
Card {
    padding: 2mm;
    margin: 1mm 2mm 3mm;
}
```

Use shorthand (`padding`, `margin`) or side-specific forms (`padding-left`, `margin-top`).

## Borders

```css
ToolbarButton {
    border: 1pt solid #3399ff;
    border-radius: 1mm;
}
```

* Mixing widths or colors on individual sides may trigger image border generation.
* Set `border: none;` to disable an inherited image border.

## Backgrounds

```css
HeroBanner {
    background: linear-gradient(0deg, #ccc, #666);
    cn1-background-type: cn1-image-scaled-fill;
}
```

* Gradients with two stops and cardinal directions compile to native gradients; others become generated images.
* Image borders override `background` declarations unless disabled.

## Typography

```css
Label {
    font-family: "native:MainRegular";
    font-size: 2.2mm;
    font-style: italic;
    font-weight: bold;
    color: #222;
    text-decoration: underline;
}
```

Codename One extends `text-decoration` with `cn1-3d`, `cn1-3d-lowered`, and `cn1-3d-shadow-north` for embossed text effects.

## Effects and Utility

* `opacity: 0.5;`
* `box-shadow: 0 1mm 2mm rgba(0, 0, 0, 0.25);`
* `width`/`height`: respected when generating background or border images.

Refer to the other topic guides for Codename One–specific extensions such as `cn1-source-dpi` and `cn1-background-type`.
