package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.CssCompileRequest;
import com.codename1.server.mcp.dto.CssCompileResponse;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.tools.GlobalExtractor;
import com.codename1.server.mcp.tools.Jdk8ManagerFromResource;
import com.codename1.server.mcp.util.OsUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Compiles Codename One CSS resources by invoking the desktop designer tooling. */
@Service
public class CssCompileService {
  private static final Logger LOG = LoggerFactory.getLogger(CssCompileService.class);
  private static final Pattern URL_PATTERN = Pattern.compile("url\\(([^)]+)\\)");
  private static final byte[] STUB_PNG =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8"
                  + "/x8AAwMB/YoLYiQAAAAASUVORK5CYII=");
  private static final byte[] STUB_JPEG =
      Base64.getDecoder()
          .decode(
              "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////"
                  + "////////////////////////////////////////////////////////////"
                  + "////////////////////////////////////////////////////////////"
                  + "////2wBDAf//////////////////////////////////////////////////"
                  + "////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB"
                  + "/8QAFQABAQAAAAAAAAAAAAAAAAAAAAf/xAAUEQEAAAAAAAAAAAAAAAAAAAAA"
                  + "/9oADAMBAAIQAxAAAAF+AP/EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEA"
                  + "AT8Af//EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQIBAT8Af//EABQRAQAA"
                  + "AAAAAAAAAAAAAAAAAAD/2gAIAQMBAT8Af//Z");
  private static final byte[] STUB_SVG =
      "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1\" height=\"1\"></svg>"
          .getBytes(StandardCharsets.UTF_8);

  private final GlobalExtractor extractor;
  private final Jdk8ManagerFromResource jdk8;

  private final Lock fontStubLock = new ReentrantLock();
  private final AtomicReference<byte[]> fontStub = new AtomicReference<>();

  public CssCompileService(GlobalExtractor extractor, Jdk8ManagerFromResource jdk8) {
    this.extractor = extractor;
    this.jdk8 = jdk8;
  }

  /**
   * Compiles the provided CSS request using the Codename One designer CLI.
   *
   * @param request the compilation inputs and options
   * @return the compilation response with logs and success flag
   */
  public CssCompileResponse compile(CssCompileRequest request) {
    Objects.requireNonNull(request, "request");
    try {
      Path designerJar = extractor.ensureFile("/cn1libs/designer.jar");
      Path javaBinary = jdk8.ensureJava8();

      Path workDir = Files.createTempDirectory("cn1css-");
      Path cssInput = null;
      List<Path> cssFiles = new ArrayList<>();
      try {
        List<FileEntry> files = request.files() == null ? List.of() : List.copyOf(request.files());
        for (FileEntry entry : files) {
          Path resolved = safeResolve(workDir, entry.path());
          Path parent = resolved.getParent();
          if (parent != null) {
            Files.createDirectories(parent);
          }
          Files.writeString(resolved, entry.content(), StandardCharsets.UTF_8);
          if (entry.path().equals(request.inputPath())) {
            cssInput = resolved;
          }
          if (entry.path().toLowerCase(Locale.ENGLISH).endsWith(".css")) {
            cssFiles.add(resolved);
          }
        }
        if (cssFiles.isEmpty()) {
          throw new IOException("No CSS files supplied for compilation");
        }

        if (cssInput == null) {
          cssInput = cssFiles.isEmpty() ? null : cssFiles.get(0);
        }
        if (cssInput == null) {
          throw new IOException("No CSS input file provided");
        }

        String outputName =
            request.outputPath() != null && !request.outputPath().isBlank()
                ? request.outputPath()
                : "theme.res";
        Path outputFile = safeResolve(workDir, outputName);
        Files.deleteIfExists(outputFile);
        Path outputParent = outputFile.getParent();
        if (outputParent != null) {
          Files.createDirectories(outputParent);
        }

        Files.createDirectories(workDir.resolve("target"));

        for (Path cssFile : cssFiles) {
          ensureCssResources(workDir, cssFile, designerJar);
        }

        List<String> command = new ArrayList<>();
        if (OsUtils.isLinux()) {
          Path xvfb = OsUtils.locateOnPath("xvfb-run");
          if (xvfb == null) {
            throw new IOException("xvfb-run command not available on PATH");
          }
          command.add(xvfb.toString());
          command.add("-a");
        }
        command.add(javaBinary.toString());
        command.add("-cp");
        command.add(designerJar.toString());
        command.add("com.codename1.designer.css.CN1CSSCLI");
        command.add("-i");
        command.add(cssInput.toString());
        command.add("-o");
        command.add(outputFile.toString());

        LOG.info("Running CSS compiler: {}", command);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String log;
        try (InputStream in = process.getInputStream()) {
          log = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        int exit = process.waitFor();
        boolean ok = exit == 0 && Files.exists(outputFile);
        LOG.info("CSS compile finished with exitCode={} ok={}", exit, ok);
        return new CssCompileResponse(ok, log);
      } finally {
        cleanup(workDir);
      }
    } catch (IOException e) {
      LOG.error("CSS compile failed", e);
      return new CssCompileResponse(false, e.toString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.error("CSS compile interrupted", e);
      return new CssCompileResponse(false, e.toString());
    }
  }

  private Path safeResolve(Path root, String relative) throws IOException {
    Path resolved = root.resolve(relative).normalize();
    if (!resolved.startsWith(root)) {
      throw new IOException("Attempt to escape workspace for path " + relative);
    }
    return resolved;
  }

  private void ensureCssResources(Path workRoot, Path cssFile, Path designerJar)
      throws IOException {
    String css = Files.readString(cssFile, StandardCharsets.UTF_8);
    Matcher matcher = URL_PATTERN.matcher(css);
    while (matcher.find()) {
      String raw = matcher.group(1).trim();
      if (raw.isEmpty()) {
        continue;
      }
      if (raw.startsWith("data:")) {
        continue;
      }
      if (raw.startsWith("#")) {
        continue;
      }
      String cleaned = raw;
      if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
          || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
        cleaned = cleaned.substring(1, cleaned.length() - 1);
      }
      if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
        continue;
      }
      Path cssParent = cssFile.getParent();
      if (cssParent == null) {
        cssParent = cssFile.getFileSystem().getPath("");
      }
      Path resolved = safeResolve(workRoot, cssParent.resolve(cleaned).toString());
      if (Files.exists(resolved)) {
        continue;
      }
      Path parent = resolved.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      byte[] content = stubContentFor(cleaned, designerJar);
      Files.write(
          resolved, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
  }

  private byte[] stubContentFor(String path, Path designerJar) throws IOException {
    String lower = path.toLowerCase(Locale.ENGLISH);
    if (lower.endsWith(".png") || lower.endsWith(".gif")) {
      return STUB_PNG;
    }
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return STUB_JPEG;
    }
    if (lower.endsWith(".svg")) {
      return STUB_SVG;
    }
    if (lower.endsWith(".ttf") || lower.endsWith(".otf")) {
      return loadFontStub(designerJar);
    }
    return new byte[0];
  }

  private byte[] loadFontStub(Path designerJar) throws IOException {
    byte[] cached = fontStub.get();
    if (cached != null) {
      return cached;
    }
    fontStubLock.lock();
    try {
      byte[] existing = fontStub.get();
      if (existing != null) {
        return existing;
      }
      try (InputStream in = Files.newInputStream(designerJar);
          ZipInputStream zip = new ZipInputStream(in)) {
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
          if (!entry.isDirectory()
              && entry.getName().equals("com/codename1/impl/javase/Roboto-Regular.ttf")) {
            byte[] data = zip.readAllBytes();
            fontStub.set(data);
            return data;
          }
        }
      }
      throw new IOException("Failed to locate Roboto-Regular.ttf inside designer.jar");
    } finally {
      fontStubLock.unlock();
    }
  }

  private void cleanup(Path dir) {
    if (dir == null) {
      return;
    }
    try {
      Files.walk(dir)
          .sorted((a, b) -> b.compareTo(a))
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException ex) {
                  // SpotBugs: log cleanup issues instead of silently swallowing them.
                  LOG.debug("Failed to delete temporary path {}", path, ex);
                }
              });
    } catch (IOException ex) {
      // SpotBugs: deletion failures should be visible during troubleshooting but not fatal.
      LOG.debug("Failed to walk temporary directory {}", dir, ex);
    }
  }
}
