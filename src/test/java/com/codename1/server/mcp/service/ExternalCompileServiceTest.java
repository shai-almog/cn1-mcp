package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.tools.GlobalExtractor;
import com.codename1.server.mcp.tools.Jdk8ManagerFromResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.*;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExternalCompileServiceTest {

    static class FsBackedExtractor extends GlobalExtractor {
        private final Path cn1;
        private final Path boot;
        FsBackedExtractor(Path cacheDir, Path cn1, Path boot) {
            super(cacheDir.toString(), "v1");
            this.cn1 = cn1;
            this.boot = boot;
        }
        @Override public Path ensureFile(String resourcePath) {
            if (resourcePath.endsWith("CodenameOne.jar")) return cn1;
            if (resourcePath.endsWith("CLDC11.jar")) return boot;
            throw new IllegalArgumentException(resourcePath);
        }
    }

    private static Path copyLib(Path tmp, String name) throws Exception {
        Path resource = Paths.get("src/main/resources/cn1libs").resolve(name);
        Path target = tmp.resolve(name);
        Files.copy(resource, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private static Path locateSystemJavac() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).contains("win");
        String binaryName = windows ? "javac.exe" : "javac";
        Path javaHome = Paths.get(System.getProperty("java.home"));
        Path candidate = javaHome.resolve("bin").resolve(binaryName);
        if (!Files.exists(candidate)) {
            candidate = javaHome.getParent().resolve("bin").resolve(binaryName);
        }
        if (!Files.exists(candidate)) {
            throw new IllegalStateException("Unable to locate system javac binary at " + candidate);
        }
        return candidate;
    }

    @Test
    void passesStrictFlagsAndClasspath(@TempDir Path tmp) throws Exception {
        // copy the production jars into our temp workspace
        Path cn1 = copyLib(tmp, "CodenameOne.jar");
        Path boot = copyLib(tmp, "CLDC11.jar");

        // extractor backed by those files
        var extractor = new FsBackedExtractor(tmp, cn1, boot);

        Path javac = locateSystemJavac();

        // JDK manager mocked to return our binary
        var jdkMgr = Mockito.mock(Jdk8ManagerFromResource.class);
        when(jdkMgr.ensureJavac8()).thenReturn(javac);

        var svc = new ExternalCompileService(extractor, jdkMgr);

        var req = new CompileRequest(List.of(
                new FileEntry("src/X.java", """
                        import java.util.ArrayList;
                        import java.util.List;

                        class X {
                            void m() {
                                List list = new ArrayList();
                                list.add("x");
                            }
                        }
                        """.stripIndent())
        ), null);

        var resp = svc.compile(req);
        assertTrue(resp.ok(), () -> "Compiler out: " + resp.javacOutput());

        assertTrue(resp.javacOutput().toLowerCase(Locale.ENGLISH).contains("warning: [unchecked]"));
    }
}
