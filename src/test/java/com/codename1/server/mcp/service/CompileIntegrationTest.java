package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.tools.GlobalExtractor;
import com.codename1.server.mcp.tools.Jdk8ManagerFromResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;

public class CompileIntegrationTest {
    private static final Path SHARED_CACHE = Paths.get(System.getProperty("java.io.tmpdir"), "cn1-mcp-it-cache");
    private static final String LINUX_RESOURCE = "/cn1libs/OpenJDK8U-jdk_x64_linux_hotspot_8u462b08.tar.gz";
    private static final String MAC_URL = "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u382-b05/OpenJDK8U-jdk_x64_mac_hotspot_8u382b05.tar.gz";
    private static final String WINDOWS_URL = "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u382-b05/OpenJDK8U-jdk_x64_windows_hotspot_8u382b05.zip";

    Path cacheDir;
    GlobalExtractor extractor;
    Jdk8ManagerFromResource jdkMgr;

    @BeforeAll
    static void ensureSharedCache() throws Exception {
        Files.createDirectories(SHARED_CACHE);
    }

    @BeforeEach
    void setup() {
        cacheDir = SHARED_CACHE.resolve(safeOsName());
        try {
            Files.createDirectories(cacheDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        extractor = new GlobalExtractor(cacheDir.toString(), "it-v1");
        assumeTrue(resourceExists("/cn1libs/CodenameOne.jar"),
                "Missing bundled CodenameOne.jar under /cn1libs/");
        jdkMgr = new Jdk8ManagerFromResource(extractor,
                LINUX_RESOURCE,
                MAC_URL,
                WINDOWS_URL,
                "release");
    }

    @Test
    void compilesWithRealCn1AndTargetsJava8_thenModernSyntaxFails() throws Exception {
        // 1) Ensure JDK8 javac from bundled/downloaded archive
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

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void ensuresWindowsJdkCanBeDownloaded() throws Exception {
        Path javac = jdkMgr.ensureJavac8();
        assertTrue(Files.exists(javac));
        assertEquals("javac.exe", javac.getFileName().toString());
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void ensuresMacJdkCanBeDownloaded() throws Exception {
        Path javac = jdkMgr.ensureJavac8();
        assertTrue(Files.isExecutable(javac));
        assertEquals("javac", javac.getFileName().toString());
        assertTrue(javac.toString().contains("Contents/Home"), "macOS JDK root should include Contents/Home");
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

    private static String safeOsName() {
        return System.getProperty("os.name", "generic").replaceAll("[^A-Za-z0-9]+", "-").toLowerCase();
    }
}
