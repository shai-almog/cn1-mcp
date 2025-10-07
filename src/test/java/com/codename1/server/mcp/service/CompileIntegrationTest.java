package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.tools.GlobalExtractor;
import com.codename1.server.mcp.tools.Jdk8ManagerFromResource;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;

public class CompileIntegrationTest {
    @TempDir Path tmp;
    Path cacheDir;
    GlobalExtractor extractor;
    Jdk8ManagerFromResource jdkMgr;

    static final String MAC_URL = "https://downloads.test/jdk/mac.tar.gz";
    static final String WINDOWS_URL = "https://downloads.test/jdk/win.zip";

    @BeforeEach
    void setup() {
        cacheDir = tmp.resolve(".cn1-mcp");           // isolated global cache per test run
        extractor = new TestGlobalExtractor(cacheDir, Map.of(
                MAC_URL, macArchive(),
                WINDOWS_URL, windowsArchive()
        ));
        assumeTrue(resourceExists("/cn1libs/CodenameOne.jar"),
                "Missing bundled CodenameOne.jar under /cn1libs/");
        jdkMgr = new Jdk8ManagerFromResource(extractor,
                "/cn1libs/OpenJDK8U-jdk_x64_linux_hotspot_8u462b08.tar.gz",
                MAC_URL,
                WINDOWS_URL,
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

    @Test
    void ensuresWindowsJdkCanBeDownloaded() throws Exception {
        withOsName("Windows 11", () -> {
            Path javac = jdkMgr.ensureJavac8();
            assertTrue(Files.exists(javac));
            assertEquals("javac.exe", javac.getFileName().toString());
        });
    }

    @Test
    void ensuresMacJdkCanBeDownloaded() throws Exception {
        withOsName("Mac OS X", () -> {
            Path javac = jdkMgr.ensureJavac8();
            assertTrue(Files.isExecutable(javac));
            assertEquals("javac", javac.getFileName().toString());
            assertTrue(javac.toString().contains("Contents/Home"), "macOS JDK root should include Contents/Home");
        });
    }

    // ---- helpers ----

    private byte[] macArchive() {
        try {
            return makeMacJdkTarGz();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] windowsArchive() {
        try {
            return makeWindowsJdkZip();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    private void withOsName(String value, ThrowingRunnable runnable) throws Exception {
        String original = System.getProperty("os.name");
        if (value != null) {
            System.setProperty("os.name", value);
        } else {
            System.clearProperty("os.name");
        }
        try {
            runnable.run();
        } finally {
            if (original != null) {
                System.setProperty("os.name", original);
            } else {
                System.clearProperty("os.name");
            }
        }
    }

    private static byte[] makeMacJdkTarGz() throws Exception {
        var baos = new ByteArrayOutputStream();
        try (var gzo = new GzipCompressorOutputStream(baos);
             var tar = new TarArchiveOutputStream(gzo)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            writeTarEntry(tar, "jdk8-mac/Contents/Home/release", "IMPLEMENTOR=\"Test\"\n", 0644);
            writeTarEntry(tar, "jdk8-mac/Contents/Home/bin/javac", "#!/bin/sh\necho mac-javac\n", 0755);
        }
        return baos.toByteArray();
    }

    private static byte[] makeWindowsJdkZip() throws Exception {
        var baos = new ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(baos)) {
            writeZipDir(zip, "jdk8-win/");
            writeZipDir(zip, "jdk8-win/bin/");
            writeZipFile(zip, "jdk8-win/release", "IMPLEMENTOR=\"Test\"\n".getBytes());
            writeZipFile(zip, "jdk8-win/bin/javac.exe", "@echo off\r\necho win-javac\r\n".getBytes());
        }
        return baos.toByteArray();
    }

    private static void writeTarEntry(TarArchiveOutputStream tar, String name, String content, int mode) throws Exception {
        byte[] bytes = content.getBytes();
        var entry = new TarArchiveEntry(name);
        entry.setSize(bytes.length);
        entry.setMode(mode);
        tar.putArchiveEntry(entry);
        tar.write(bytes);
        tar.closeArchiveEntry();
    }

    private static void writeZipDir(java.util.zip.ZipOutputStream zip, String name) throws Exception {
        var entry = new java.util.zip.ZipEntry(name);
        entry.setSize(0);
        entry.setTime(java.time.Instant.now().toEpochMilli());
        zip.putNextEntry(entry);
        zip.closeEntry();
    }

    private static void writeZipFile(java.util.zip.ZipOutputStream zip, String name, byte[] bytes) throws Exception {
        var entry = new java.util.zip.ZipEntry(name);
        entry.setSize(bytes.length);
        entry.setTime(java.time.Instant.now().toEpochMilli());
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private static class TestGlobalExtractor extends GlobalExtractor {
        private final Map<String, byte[]> downloads;

        TestGlobalExtractor(Path cacheDir, Map<String, byte[]> downloads) {
            super(cacheDir.toString(), "it-v1");
            this.downloads = downloads;
        }

        @Override
        protected InputStream openUrl(String url) throws java.io.IOException {
            byte[] bytes = downloads.get(url);
            if (bytes != null) {
                return new ByteArrayInputStream(bytes);
            }
            return super.openUrl(url);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
