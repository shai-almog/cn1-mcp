package com.codename1.server.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.ScaffoldRequest;
import com.codename1.server.mcp.dto.ScaffoldResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScaffoldServiceTest {

  private final ScaffoldService service = new ScaffoldService();

  @Test
  void scaffoldGeneratesExpectedFiles() {
    ScaffoldRequest request = new ScaffoldRequest("My App", "com.example.demo", List.of());

    ScaffoldResponse response = service.scaffold(request);

    assertThat(response.files()).hasSize(5);
    assertThat(response.files())
        .extracting(FileEntry::path)
        .containsExactlyInAnyOrder(
            "pom.xml",
            "src/main/java/com/example/demo/MyApplication.java",
            "src/main/codenameone/native/android/build.gradle",
            "src/main/codenameone/theme.css",
            "src/main/codenameone/codenameone_settings.properties");

    String pom = findContent(response, "pom.xml");
    assertThat(pom).contains("<artifactId>cn1app</artifactId>");

    String javaSource = findContent(response, "src/main/java/com/example/demo/MyApplication.java");
    assertThat(javaSource)
        .contains("package com.example.demo;")
        .contains("new Form(\"My App\"")
        .contains("btn.setUIID(\"PrimaryButton\");");

    String css = findContent(response, "src/main/codenameone/theme.css");
    assertThat(css).contains(".PrimaryButton");

    String settings = findContent(response, "src/main/codenameone/codenameone_settings.properties");
    assertThat(settings).contains("codename1.cssTheme=theme.css");
  }

  private static String findContent(ScaffoldResponse response, String path) {
    return response.files().stream()
        .filter(entry -> entry.path().equals(path))
        .map(FileEntry::content)
        .findFirst()
        .orElseThrow();
  }
}
