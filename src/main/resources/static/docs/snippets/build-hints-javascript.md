---
topic: build-hints
title: JavaScript port build hints
description: Control proxy injection, HTML head customization, and build strictness for the Codename One JavaScript target.
---
```properties
# Proxy configuration for HTTP requests
javascript.inject_proxy=true
javascript.proxy.url=https://example.com/proxy

# Inject additional markup in index.html
javascript.inject.beforeHead=<meta name="viewport" content="width=device-width, initial-scale=1">
javascript.inject.afterHead=<script src="analytics.js"></script>

# Build output behavior
javascript.minifying=true
javascript.sourceFilesCopied=false
javascript.stopOnErrors=true
javascript.teavm.version=0.8.1
```
