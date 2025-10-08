# Java Integration

These snippets illustrate how to apply Codename One CSS UIIDs and assets from your code.

## Applying Custom UIIDs

```java
Button primary = new Button("Submit");
primary.setUIID("MyPrimaryButton");
```

Match `MyPrimaryButton` to a selector defined in your CSS file.

## Loading Resources

```java
Resources theme = Resources.openLayered("/theme");
Image logo = theme.getImage("NowLogo.png");
Label header = new Label(logo);
```

`openLayered` loads the CSS-compiled `.res` file generated under the `src` directory.

## Multi-Image Assets

Retrieve density-aware images by the logical name declared in CSS or `#Constants`:

```java
Image menuIcon = theme.getImage("menu.png");
Button menu = new Button(menuIcon);
```

## Applying Theme Constants

```java
boolean centeredPopup = theme.getBoolean("centeredPopupBool", false);
UIManager.getInstance().getLookAndFeel().setDefaultDecorationTransitionName(
        theme.getString("dialogTransitionIn", "fade"));
```

## Styling Containers Dynamically

```java
Container card = new Container(BoxLayout.y());
card.setUIID(Display.getInstance().isTablet() ? "TabletCard" : "Card");
card.getAllStyles().setMarginUnit(Style.UNIT_TYPE_MM);
card.getAllStyles().setMargin(1, 1, 1, 1);
```

Switching UIIDs at runtime lets you reuse the same CSS while adjusting layout for different devices.
