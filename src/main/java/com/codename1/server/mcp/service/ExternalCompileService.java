package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.CompileResponse;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.tools.GlobalExtractor;
import com.codename1.server.mcp.tools.Jdk8ManagerFromResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
public class ExternalCompileService {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalCompileService.class);
    private final GlobalExtractor extractor;
    private final Jdk8ManagerFromResource jdk8;

    public ExternalCompileService(GlobalExtractor extractor, Jdk8ManagerFromResource jdk8) {
        this.extractor = extractor;
        this.jdk8 = jdk8;
    }

    public CompileResponse compile(CompileRequest req) {
        try {
            List<FileEntry> files =
                    req.files() != null ? req.files() : List.of();
            LOG.info("Compile request received with {} file(s)", files.size());
            // Extract libs once
            Path cn1   = extractor.ensureFile("/cn1libs/CodenameOne.jar");
            Path boot  = extractor.ensureFile("/cn1libs/CLDC11.jar");
            LOG.debug("Compiler dependencies ready: cn1Jar={}, bootJar={}", cn1, boot);

            // Use bundled JDK 8
            Path javac = jdk8.ensureJavac8();
            LOG.debug("Resolved javac path at {}", javac);

            // Write sources
            Path work = Files.createTempDirectory("cn1c-");
            LOG.debug("Writing source files to {}", work);
            List<Path> sources = new ArrayList<>();
            for (var f : files) {
                Path p = work.resolve(f.path());
                Files.createDirectories(p.getParent());
                Files.writeString(p, f.content(), StandardCharsets.UTF_8);
                sources.add(p);
                LOG.trace("Wrote source file {}", p);
            }

            List<String> cmd = new ArrayList<>(List.of(
                    javac.toString(),
                    "-source", "8", "-target", "8",
                    "-Xlint:all",
                    "-extdirs", "",
                    "-bootclasspath", boot.toString(),
                    "-classpath", cn1.toString()
            ));
            if (Files.exists(boot)) {
                cmd.addAll(List.of("-bootclasspath", boot.toString()));
            }
            sources.forEach(s -> cmd.add(s.toString()));

            LOG.debug("Executing javac command: {}", cmd);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(work.toFile());
            pb.redirectErrorStream(true);
            Process pr = pb.start();
            String out;
            try (InputStream is = pr.getInputStream()) {
                out = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            int code = pr.waitFor();
            LOG.debug("javac exited with code {}", code);
            boolean ok = code == 0 && !out.toLowerCase().contains("error:");
            LOG.info("Compile finished: ok={}, outputLength={}", ok, out.length());
            return new CompileResponse(ok, out, List.of());
        } catch (Exception e) {
            LOG.error("Compile failed", e);
            return new CompileResponse(false, e.toString(), List.of());
        }
    }
}