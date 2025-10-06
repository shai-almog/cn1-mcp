package com.codename1.server.mcp.service;

import com.codename1.server.mcp.tools.GlobalExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExtractorTest {

    static class TestableExtractor extends GlobalExtractor {
        private final byte[] payload;
        private final Path cacheDir;

        TestableExtractor(Path cacheDir, String versionTag, byte[] payload) {
            super(cacheDir.toString(), versionTag);
            this.cacheDir = cacheDir;
            this.payload = payload;
        }

        @Override
        protected byte[] readResource(String path) {
            return payload;
        }
    }

    @Test
    void extractsOnceWithLock(@TempDir Path tmp) throws Exception {
        byte[] data = "CN1JAR".getBytes();
        var ex = new TestableExtractor(tmp, "vZ", data);

        // fire 16 concurrent calls
        var pool = Executors.newFixedThreadPool(8);
        Callable<Path> task = () -> ex.ensureFile("/cn1libs/CodenameOne.jar");
        List<Future<Path>> futures = pool.invokeAll(java.util.Collections.nCopies(16, task));
        pool.shutdown();

        Path first = futures.get(0).get();
        for (var f : futures) assertEquals(first, f.get());
        // file exists and has exact payload
        assertArrayEquals(Files.readAllBytes(first), data);
    }
}