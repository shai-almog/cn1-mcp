package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.tools.GlobalExtractor;
import com.codename1.server.mcp.tools.Jdk8ManagerFromResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;

public class CompileIntegrationTest {
    @TempDir Path tmp;
    Path cacheDir;
    GlobalExtractor extractor;
    Jdk8ManagerFromResource jdkMgr;

    @BeforeEach
    void setup() {
        cacheDir = tmp.resolve(".cn1-mcp");           // isolated global cache per test run
        extractor = new GlobalExtractor(cacheDir.toString(), "it-v1"); // use production extractor
        assumeTrue(resourceExists("/cn1libs/CodenameOne.jar"),
                "Missing bundled CodenameOne.jar under /cn1libs/");
        jdkMgr = new Jdk8ManagerFromResource(extractor,
                "/cn1libs/OpenJDK8U-jdk_x64_linux_hotspot_8u462b08.tar.gz",
                "",
                "",
                "release");
    }

    @Test
    void compilesWithRealCn1AndTargetsJava8_thenModernSyntaxFails() throws Exception {
        // 1) Ensure JDK8 javac from bundled tarball
        Path javac = jdkMgr.ensureJavac8();
        assertTrue(Files.isExecutable(javac), "javac must be executable from bundled JDK8");

        // 2) Extract real CN1 jars from resources (once to global cache)
        Path cn1Jar   = extractor.ensureFile("/cn1libs/CodenameOne.jar");
        Path bootRt   = tryEnsure("/cn1libs/CLDC11.jar");

        // 3) Exercise the actual compile service (end-to-end with extractor+jdk manager)
        ExternalCompileService svc = new ExternalCompileService(extractor, jdkMgr);

        String cn1Ok =
                //language=java
                """
                package demo;
                import com.codename1.ui.*;
                import com.codename1.ui.layouts.BorderLayout;
                public class MyApp {
                  public void start() {
                    Form f = new Form("Title", new BorderLayout());
                    Button b = new Button("Hi");
                    b.setUIID("PrimaryButton");
                    f.add(BorderLayout.CENTER, b);
                    f.show();
                  }
                };
                """;

        var okReq = new CompileRequest(List.of(new FileEntry("demo/MyApp.java", cn1Ok)), null);
        var okRes = svc.compile(okReq);
        assertTrue(okRes.ok(), () -> "Service compile failed:\n" + okRes.javacOutput());

        // Modern Java syntax (var) should fail under -source 8
        String badRes =
                //language=java
                """
                package demo;
                import com.codename1.ui.*;
                import com.codename1.ui.layouts.BorderLayout;
                public class MyApp {
                  public void start() {
                    Form f = new Form("Title", new BorderLayout());
                    var b = new Button("Hi");
                    b.setUIID("PrimaryButton");
                    f.add(BorderLayout.CENTER, b);
                    f.show();
                  }
                };
                """;
        var failReq = new CompileRequest(List.of(new FileEntry("demo/MyApp.java", badRes)), null);
        var failRes = svc.compile(failReq);
        assertFalse(failRes.ok(), () -> "Compilation succeeded when it should have failed");
        assertFalse(failRes.javacOutput().isEmpty(), () -> "Compilation error list is empty");
    }

    // ---- helpers ----

    private static boolean resourceExists(String path) {
        try (InputStream in = CompileIntegrationTest.class.getResourceAsStream(path)) {
            return in != null;
        } catch (Exception e) {
            return false;
        }
    }

    private Path tryEnsure(String resourcePath) {
        try {
            return extractor.ensureFile(resourcePath);
        } catch (Exception e) {
            return null; // optional resource not present
        }
    }
}
