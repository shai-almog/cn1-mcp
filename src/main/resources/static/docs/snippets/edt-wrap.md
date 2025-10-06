```java
import com.codename1.ui.CN;

public class Snippet {
  public static void runInUIThread(Runnable r) {
    CN.callSerially(r);
  }
}
```