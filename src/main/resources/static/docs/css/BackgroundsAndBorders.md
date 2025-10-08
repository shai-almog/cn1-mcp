# Backgrounds and Borders

Codename One renders many borders natively, but complex shapes fall back to generated 9-piece images at build time.

## Round Borders

### Built-in Styles

```css
RoundButton {
    border: 1px #3399ff cn1-round-border;
    padding: 3mm;
}

PillButton {
    border: 1pt #3399ff cn1-pill-border;
    color: white;
    background: cn1-pill-border;
    background-color: #3399ff;
}
```

`cn1-round-border` and `cn1-pill-border` avoid generating image borders and are efficient for circular or pill shapes.

### `border-radius`

```css
Badge {
    background-color: red;
    border-radius: 2mm;
    padding: 1mm 2mm;
}
```

If the declaration can be represented with `RoundRectBorder`, the compiler keeps it native; otherwise it generates a 9-piece image.

## Shadows on Round Borders

Use the Codename One shadow extensions instead of standard `box-shadow` to avoid image generation.

```css
PillButton {
    border: 1pt #3399ff cn1-pill-border;
    cn1-box-shadow-color: rgba(0, 0, 0, 0.35);
    cn1-box-shadow-spread: 1mm;
    cn1-box-shadow-h: 0;
    cn1-box-shadow-v: 1mm;
    cn1-box-shadow-blur: 2mm;
}
```

## Image Borders

```css
Card {
    border-image: url('dashbg_landscape.png');
    border-image-slice: 10% 30% 40% 20%;
}
```

Omit `border-image-slice` to use the default 40% inset.

## Background Layers

Priority order:

1. Image border (covers the entire component)
2. Background image (`background-image` / `background`)
3. Background color (`background-color`)

Disable higher layers if you want lower ones to show through:

```css
Card {
    border: none;
    background-color: #f5f5f5;
}
```

## Gradient Support

Native linear gradients must have exactly two color stops with matching alpha, and a direction of `0deg`, `90deg`, `180deg`, or `270deg` (or matching keywords).

```css
Banner {
    background: linear-gradient(to top, #ccc, #666);
}
```

Other gradients generate background images during compilation.

Radial gradients render natively when the first stop starts at `0%`.

```css
Badge {
    background: radial-gradient(circle at left, gray, white);
}
```
