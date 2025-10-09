---
topic: rest
title: GET JSON with the Rest API
description: Fetch JSON asynchronously and handle the response off the EDT before updating the UI.
---
```java
import com.codename1.io.rest.Rest;

public class Snippet {
  public static void fetch(String url) {
    Rest.get(url).acceptJson().onComplete(res -> {
        // Use res.getResponseData()
    }).send();
  }
}
```
