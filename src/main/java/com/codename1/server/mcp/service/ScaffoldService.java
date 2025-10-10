package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.ScaffoldRequest;
import com.codename1.server.mcp.dto.ScaffoldResponse;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Builds Codename One starter projects based on scaffold requests. */
@Service
public class ScaffoldService {
  private static final Logger LOG = LoggerFactory.getLogger(ScaffoldService.class);

  /**
   * Generates a scaffold project for the provided request.
   *
   * @param req scaffold options supplied by the caller
   * @return a response containing files for the new project skeleton
   */
  public ScaffoldResponse scaffold(ScaffoldRequest req) {
    String pkg = req.pkg();
    String name = req.name();

    LOG.info("Generating scaffold for package={} name={}", pkg, name);

    var files = new ArrayList<FileEntry>();
    files.add(new FileEntry("pom.xml", pom()));
    files.add(
        new FileEntry(
            "src/main/java/" + pkg.replace('.', '/') + "/MyApplication.java", appJava(pkg, name)));
    files.add(new FileEntry("src/main/codenameone/native/android/build.gradle", "// placeholder"));
    files.add(new FileEntry("src/main/codenameone/theme.css", themeCss()));
    files.add(new FileEntry("src/main/codenameone/codenameone_settings.properties", settings()));
    ScaffoldResponse response = new ScaffoldResponse(files);
    LOG.info("Scaffold generated with {} files", response.files().size());
    return response;
  }

  /**
   * Produces the Maven project descriptor used by the scaffold.
   *
   * @return the pom.xml content
   */
  private String pom() {
    return """
            <!-- Minimal CN1 Maven skeleton; fill versions/plugins to match your setup -->
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId><artifactId>cn1app</artifactId><version>1.0.0</version>
              <properties><maven.compiler.source>11</maven.compiler.source><maven.compiler.target>8</maven.compiler.target></properties>
              <dependencies>
                <dependency>
                  <groupId>com.codenameone</groupId>
                  <artifactId>CodenameOne</artifactId>
                  <version>latest</version>
                  <scope>provided</scope>
                </dependency>
              </dependencies>
            </project>
            """;
  }

  /**
   * Generates the Java entry point class for the scaffolded project.
   *
   * @param pkg the Java package name
   * @param name the application display name
   * @return the Java source file contents
   */
  private String appJava(String pkg, String name) {
    String template =
        """
            package %s;
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.BorderLayout;

            public class MyApplication {
              private Form current;
              public void init(Object context) { }

              public void start() {
                if(current != null) { current.show(); return; }
                Form f = new Form("%s", new BorderLayout());
                Button btn = new Button("Hello CN1");
                btn.setUIID("PrimaryButton");
                f.add(BorderLayout.CENTER, btn);
                f.show();
              }

              public void stop() { current = Display.getInstance().getCurrent(); }
              public void destroy() { }
            }
            """;
    // SpotBugs: use %n in formatted strings to honour platform-specific line endings.
    return String.format(template.replace("\n", "%n"), pkg, name);
  }

  /**
   * Creates a sample CSS theme for the scaffolded application.
   *
   * @return the CSS file contents
   */
  private String themeCss() {
    return """
            /* Codename One CSS: compiled to theme.res */
            Form { bgColor: white; }
            .PrimaryButton {
              color: white;
              bgColor: #3A86FF;
              padding: 2mm;
              border-radius: 2mm;
              margin: 2mm;
            }
            """;
  }

  /**
   * Provides the default Codename One settings configuration.
   *
   * @return the codenameone_settings.properties content
   */
  private String settings() {
    return """
            codename1.android.keystore=none
            codename1.ios.provision=none
            codename1.cssTheme=theme.css
            """;
  }
}
