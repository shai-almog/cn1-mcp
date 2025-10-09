---
topic: build-hints
title: Desktop build hints
description: Size Codename One desktop apps, select bundled JVMs, and inject native desktop manifest values.
---
```properties
# Window dimensions and behavior
desktop.width=800
desktop.height=600
desktop.adaptToRetina=true
desktop.resizable=true

# Desktop themes
desktop.theme=native
desktop.themeMac=ios
desktop.themeWin=win

# High level packaging
desktop.windowsOutput=exe

# Mac/Windows media backends
desktop.win.cef=false
desktop.mac.cef=false

# macOS Info.plist injection
desktop.mac.plist.LSApplicationCategoryType=public.app-category.business
desktop.mac.plistInject=<key>LSBackgroundOnly</key><true/>

# Windows manifest extensions
windows.extensions=<Extensions>...</Extensions>

# Bundle custom JVMs
mac.desktop-vm=zuluFx8
win.desktop-vm=zulu11
win.vm32bit=false
```
