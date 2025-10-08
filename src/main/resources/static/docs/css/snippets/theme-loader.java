Resources theme = Resources.openLayered("/theme");
Form home = new Form("Home", BoxLayout.y());

Button cta = new Button("Continue");
cta.setUIID("MyPrimaryButton");
cta.addActionListener(e -> Dialog.show("Clicked", "Primary action", "OK", null));

home.addAll(new Label(theme.getImage("menu.png")), cta);
home.show();
