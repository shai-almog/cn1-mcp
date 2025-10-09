---
topic: threading
title: Run logic on the Codename One EDT
description: Use CN.callSerially to ensure UI changes execute on the Codename One event dispatch thread.
---
```java
import com.codename1.ui.CN;

public class Snippet {
  public static void runInUIThread(Runnable r) {
    CN.callSerially(r);
  }
}
```
