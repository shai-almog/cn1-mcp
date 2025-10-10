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

/** Compiles Java code by invoking the bundled JDK 8 compiler. */
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
   * Runs javac against the provided source files.
   *
   * @param req compile request describing the files to compile
   * @return the response containing compiler output and status
   */
  public CompileResponse compile(CompileRequest req) {
    try {
      List<FileEntry> files = req.files() == null ? List.of() : List.copyOf(req.files());
      LOG.info("Starting compile request with {} files", files.size());

      Path cn1 = extractor.ensureFile("/cn1libs/CodenameOne.jar");
      Path boot = extractor.ensureFile("/cn1libs/CLDC11.jar");

      Path javac = jdk8.ensureJavac8();
      LOG.debug("Resolved javac path: {}", javac);

      Path work = Files.createTempDirectory("cn1c-");
      List<Path> sources = new ArrayList<>();
      for (FileEntry f : files) {
        Path path = work.resolve(f.path());
        Path parent = path.getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }
        // SpotBugs: guard against files placed at the workspace root which have no parent
        // directory.
        Files.writeString(path, f.content(), StandardCharsets.UTF_8);
        sources.add(path);
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
      sources.forEach(source -> cmd.add(source.toString()));
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
