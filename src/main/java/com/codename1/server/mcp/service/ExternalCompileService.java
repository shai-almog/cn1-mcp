package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.CompileResponse;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.tools.GlobalExtractor;
import com.codename1.server.mcp.tools.Jdk8ManagerFromResource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Compiles user-provided Java sources using the embedded Codename One toolchain. */
@Service
public class ExternalCompileService {
  private static final Logger LOG = LoggerFactory.getLogger(ExternalCompileService.class);

  private final GlobalExtractor extractor;
  private final Jdk8ManagerFromResource jdk8;

  public ExternalCompileService(GlobalExtractor extractor, Jdk8ManagerFromResource jdk8) {
    this.extractor = extractor;
    this.jdk8 = jdk8;
  }

  /**
   * Compiles the provided Java sources and returns the javac output and status.
   */
  public CompileResponse compile(CompileRequest req) {
    try {
      List<FileEntry> files = req.files() == null ? List.of() : List.copyOf(req.files());
      LOG.info("Starting compile request with {} files", files.size());

      // Extract libs once
      Path cn1 = extractor.ensureFile("/cn1libs/CodenameOne.jar");
      Path boot = extractor.ensureFile("/cn1libs/CLDC11.jar");

      // Use bundled JDK 8
      Path javac = jdk8.ensureJavac8();
      LOG.debug("Resolved javac path: {}", javac);

      // Write sources
      Path work = Files.createTempDirectory("cn1c-");
      List<Path> sources = new ArrayList<>();
      for (var f : files) {
        Path p = work.resolve(f.path());
        Path parent = p.getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }
        // SpotBugs: guard against files placed at the workspace root which have no parent
        // directory.
        Files.writeString(p, f.content(), StandardCharsets.UTF_8);
        sources.add(p);
      }

      List<String> cmd =
          new ArrayList<>(
              List.of(
                  javac.toString(),
                  "-source",
                  "8",
                  "-target",
                  "8",
                  "-Xlint:all",
                  "-extdirs",
                  "",
                  "-classpath",
                  cn1.toString()));
      if (Files.exists(boot)) {
        cmd.addAll(List.of("-bootclasspath", boot.toString()));
      }
      sources.forEach(s -> cmd.add(s.toString()));
      LOG.debug("Compile command: {}", cmd);

      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb.directory(work.toFile());
      pb.redirectErrorStream(true);
      Process pr = pb.start();
      String out;
      try (InputStream is = pr.getInputStream()) {
        out = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }
      int code = pr.waitFor();
      boolean ok = code == 0 && !out.toLowerCase(Locale.ENGLISH).contains("error:");
      LOG.info("Compile finished with exitCode={} success={}", code, ok);
      return new CompileResponse(ok, out, List.of());
    } catch (IOException e) {
      LOG.error("Compile failed", e);
      return new CompileResponse(false, e.toString(), List.of());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.error("Compile interrupted", e);
      return new CompileResponse(false, e.toString(), List.of());
    }
  }
}
