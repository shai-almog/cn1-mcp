package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.tools.GlobalExtractor;
import com.codename1.server.mcp.tools.Jdk8ManagerFromResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

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

    private static Path makeFakeJar(Path dir, String name) throws Exception {
        Path p = dir.resolve(name);
        Files.writeString(p, "jar", StandardCharsets.UTF_8);
        return p;
    }

    private static Path makeJavacShim(Path dir, Path argsOut) throws Exception {
        Path sh = dir.resolve("javac");
        String script = """
      #!/bin/sh
      printf "ARGS:" > "%s"
      for a in "$@"; do printf " [%s]" "$a" >> "%s"; done
      printf "\\n" >> "%s"
      exit 0
      """.formatted(argsOut, "%s", argsOut, argsOut);
        Files.writeString(sh, script);
        sh.toFile().setExecutable(true);
        return sh;
    }

    @Test
    void passesStrictFlagsAndClasspath(@TempDir Path tmp) throws Exception {
        // fake jars
        Path cn1 = makeFakeJar(tmp, "CodenameOne.jar");
        Path boot = makeFakeJar(tmp, "CLDC11.jar");

        // extractor backed by those files
        var extractor = new FsBackedExtractor(tmp, cn1, boot);

        // fake jdk folder with javac shim
        Path jdkDir = Files.createDirectories(tmp.resolve("jdk8/bin"));
        Path argsOut = tmp.resolve("javac.args.txt");
        Path javac = makeJavacShim(jdkDir, argsOut);

        // JDK manager mocked to return our shim
        var jdkMgr = Mockito.mock(Jdk8ManagerFromResource.class);
        when(jdkMgr.ensureJavac8()).thenReturn(javac);

        var svc = new ExternalCompileService(extractor, jdkMgr);

        var req = new CompileRequest(List.of(
                new FileEntry("src/X.java", "class X{}")
        ), null);

        var resp = svc.compile(req);
        assertTrue(resp.ok(), () -> "Compiler out: " + resp.javacOutput());

        String args = Files.readString(argsOut);
        assertTrue(args.contains("[-source] [8]"));
        assertTrue(args.contains("[-target] [8]"));
        assertTrue(args.contains("[-extdirs] []"));
        assertTrue(args.contains("[-classpath] [" + cn1.toString()));
        // bootclasspath is optional; assert if present
        assertTrue(args.contains("[-bootclasspath] [" + boot.toString() + "]"));
    }
}