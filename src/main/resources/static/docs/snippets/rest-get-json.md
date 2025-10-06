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