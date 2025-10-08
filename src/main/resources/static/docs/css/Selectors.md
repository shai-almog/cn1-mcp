# Selectors

Codename One interprets CSS selectors as UIIDs rather than DOM nodes. Focus on mapping selectors to component names defined in your theme.

## UIID Selectors

```css
Button {
    padding: 2mm 3mm;
    text-align: center;
}
```

* `Button` targets the UIID named `Button` in the Codename One theme.
* Use multiple selectors to share rules:
  ```css
  Button, TextField, Form {
      font-size: 2mm;
  }
  ```

## State Selectors

Codename One supports four state pseudo-classes:

* `.pressed`
* `.selected`
* `.unselected`
* `.disabled`

```css
Button.pressed {
    background-color: #999;
}
```

If no state is specified, the selector applies to all states.

## Deriving Styles

Use `cn1-derive` to inherit declarations from another UIID and override only what you need:

```css
MyPrimaryButton {
    cn1-derive: Button;
    border-radius: 2mm;
    background-color: #004b8d;
}
```

Add the new UIID to components in Java:

```java
Button cta = new Button("Continue");
cta.setUIID("MyPrimaryButton");
```

## Grouping for Theme Variants

Combine selectors to define light and dark variants without duplicating files. For example:

```css
/* Base */
Label { color: #222; }

/* Dark variant */
Dark.Label {
    cn1-derive: Label;
    color: #fafafa;
}
```

Apply the variant UIID to forms or specific components when generating themes.
