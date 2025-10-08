package com.codename1.server.mcp.service;

import com.codename1.server.mcp.tools.GlobalExtractor;
import com.codename1.server.mcp.tools.Jdk8ManagerFromResource;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class Jdk8ManagerFromResourceTest {

    static class InMemExtractor extends GlobalExtractor {
        private final byte[] archive;
        InMemExtractor(Path cache, String version, byte[] archive) { super(cache.toString(), version); this.archive = archive; }
        @Override protected byte[] readResource(String path) { return archive; }
    }

    private static byte[] makeMiniJdkTarGz() throws IOException {
        var baos = new ByteArrayOutputStream();
        try (var gzo = new GzipCompressorOutputStream(baos);
             var tar = new TarArchiveOutputStream(gzo)) {

            writeEntry(tar, "jdk8mini/release", "IMPLEMENTOR=\"Test\"\n");
            writeEntry(tar, "jdk8mini/bin/javac", "#!/bin/sh\necho fake-javac\n");
        }
        return baos.toByteArray();
    }

    private static void writeEntry(TarArchiveOutputStream tar, String name, String content) throws IOException {
        byte[] bytes = content.getBytes();
        var e = new TarArchiveEntry(name);
        e.setSize(bytes.length);
        e.setMode(0755);
        tar.putArchiveEntry(e);
        tar.write(bytes);
        tar.closeArchiveEntry();
    }

    @Test
    void resolvesJavac(@TempDir Path tmp) throws Exception {
        byte[] tgz = makeMiniJdkTarGz();
        var ex = new InMemExtractor(tmp, "v1", tgz);
        var mgr = new Jdk8ManagerFromResource(ex, "/cn1libs/jdk8/temurin8-linux-x64.tar.gz", "release");
        withOsName("linux", () -> {
            Path javac = mgr.ensureJavac8();
            assertTrue(Files.isExecutable(javac), "javac should be executable");
            assertEquals("javac", javac.getFileName().toString());
        });
    }

    private void withOsName(String value, ThrowingRunnable runnable) throws Exception {
        String original = System.getProperty("os.name");
        System.setProperty("os.name", value);
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

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
