---
topic: build-hints
title: Cross-platform build hints
description: Apply global build toggles that affect analytics, Java version, and resource packaging across targets.
---
```properties
# Prevent Codename One from collecting install metrics
block_server_registration=false

# Select Codename One server compiler Java version
java.version=8

# Exclude extra Codename One resources to minimize app size
noExtraResources=false
```
