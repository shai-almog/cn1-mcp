package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.ScaffoldRequest;
import com.codename1.server.mcp.dto.ScaffoldResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
public class ScaffoldService {
    private static final Logger LOG = LoggerFactory.getLogger(ScaffoldService.class);
    public ScaffoldResponse scaffold(ScaffoldRequest req) {
        String pkg = req.pkg();
        String name = req.name();

        LOG.info("Generating scaffold for package={} name={}", pkg, name);

        var files = new ArrayList<FileEntry>();
        files.add(new FileEntry("pom.xml", pom()));
        files.add(new FileEntry("src/main/java/"+pkg.replace('.','/')+"/MyApplication.java", appJava(pkg, name)));
        files.add(new FileEntry("src/main/codenameone/native/android/build.gradle", "// placeholder"));
        files.add(new FileEntry("src/main/codenameone/theme.css", themeCss()));
        files.add(new FileEntry("src/main/codenameone/codenameone_settings.properties", settings()));
        ScaffoldResponse response = new ScaffoldResponse(files);
        LOG.info("Scaffold generated with {} files", response.files().size());
        return response;
    }

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

    private String appJava(String pkg, String name) {
        return """
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
            """.formatted(pkg, name);
    }

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

    private String settings() {
        return """
            codename1.android.keystore=none
            codename1.ios.provision=none
            codename1.cssTheme=theme.css
            """;
    }
}