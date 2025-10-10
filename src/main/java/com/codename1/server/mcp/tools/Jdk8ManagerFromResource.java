package com.codename1.server.mcp.tools;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Extracts bundled or remote JDK 8 archives and exposes convenience helpers for locating binaries.
 */
@Component
public class Jdk8ManagerFromResource {
  private static final Logger LOG = LoggerFactory.getLogger(Jdk8ManagerFromResource.class);

  private final GlobalExtractor extractor;
  private final String linuxArchiveResource;
  private final String macArchiveUrl;
  private final String windowsArchiveUrl;
  private final String rootMarker; // e.g. "release"

  /** Creates a manager that resolves the bundled JDK archives using Spring configuration. */
  @Autowired
  public Jdk8ManagerFromResource(
      GlobalExtractor extractor,
      @Value("${cn1.jdk8.linuxResourcePath:${cn1.jdk8.resourcePath}}") String linuxArchiveResource,
      @Value("${cn1.jdk8.macUrl:}") String macArchiveUrl,
      @Value("${cn1.jdk8.windowsUrl:}") String windowsArchiveUrl,
      @Value("${cn1.jdk8.rootMarker}") String rootMarker) {
    this.extractor = extractor;
    this.linuxArchiveResource = linuxArchiveResource;
    this.macArchiveUrl = macArchiveUrl;
    this.windowsArchiveUrl = windowsArchiveUrl;
    this.rootMarker = rootMarker;
  }

  /** Convenience constructor for unit tests that only require a Linux archive resource. */
  public Jdk8ManagerFromResource(
      GlobalExtractor extractor, String linuxArchiveResource, String rootMarker) {
    this(extractor, linuxArchiveResource, "", "", rootMarker);
  }

  /** Ensures the bundled JDK 8 archive is extracted and returns the {@code javac} binary. */
  public Path ensureJavac8() throws IOException {
    return ensureBinary("javac");
  }

  /** Ensures the bundled JDK 8 archive is extracted and returns the {@code java} binary. */
  public Path ensureJava8() throws IOException {
    return ensureBinary("java");
  }

  private Path ensureBinary(String binary) throws IOException {
    String os = System.getProperty("os.name", "linux").toLowerCase(Locale.ENGLISH);
    LOG.info("Resolving bundled JDK8 binary '{}' for operating system {}", binary, os);
    if (os.contains("win")) {
      return ensureFromUrl(windowsArchiveUrl, true, "Windows", binary);
    }
    if (os.contains("mac") || os.contains("darwin")) {
      return ensureFromUrl(macArchiveUrl, false, "macOS", binary);
    }
    return ensureFromResource(linuxArchiveResource, binary);
  }

  private Path ensureFromResource(String resource, String binary) throws IOException {
    if (resource == null || resource.isBlank()) {
      throw new IOException("No bundled JDK8 resource configured for Linux");
    }
    Path fileNamePath = Path.of(resource).getFileName();
    if (fileNamePath == null) {
      // SpotBugs: ensure we throw a friendly error instead of dereferencing a null file name.
      throw new IOException("Bundled JDK8 resource path must include a file name: " + resource);
    }
    String fileName = fileNamePath.toString();
    String folderName = stripArchiveExtension(fileName);
    GlobalExtractor.ArchiveType type = GlobalExtractor.ArchiveType.fromName(fileName);
    LOG.info("Ensuring Linux JDK8 resource {} -> folder {}", resource, folderName);
    Path jdkRoot = extractor.ensureArchiveExtracted(resource, folderName);
    Path root = findJdkRoot(jdkRoot);
    return resolveBinary(root, type == GlobalExtractor.ArchiveType.ZIP, binary);
  }

  private Path ensureFromUrl(String url, boolean windows, String label, String binary)
      throws IOException {
    if (url == null || url.isBlank()) {
      throw new IOException("No JDK8 download URL configured for " + label);
    }
    String fileName = fileName(url);
    String folderName = stripArchiveExtension(fileName);
    LOG.info("Ensuring remote JDK8 {} for {} -> folder {}", url, label, folderName);
    Path jdkRoot = extractor.ensureArchiveExtractedFromUrl(url, folderName);
    Path root = findJdkRoot(jdkRoot);
    return resolveBinary(root, windows, binary);
  }

  private Path findJdkRoot(Path base) throws IOException {
    Path resolved = findJdkRootRecursive(base, 0);
    return resolved != null ? resolved : base;
  }

  private Path findJdkRootRecursive(Path dir, int depth) throws IOException {
    if (Files.exists(dir.resolve(rootMarker))) {
      return dir;
    }
    if (depth >= 4) {
      return null;
    }
    try (var stream = Files.list(dir)) {
      for (Path child : (Iterable<Path>) stream::iterator) {
        if (!Files.isDirectory(child)) {
          continue;
        }
        Path candidate = findJdkRootRecursive(child, depth + 1);
        if (candidate != null) {
          return candidate;
        }
      }
    }
    return null;
  }

  private Path resolveBinary(Path root, boolean windows, String binary) throws IOException {
    String executableName = windows ? binary + ".exe" : binary;
    Path executable = root.resolve("bin").resolve(executableName);
    if (!Files.exists(executable)) {
      throw new IOException(binary + " not found at " + executable);
    }
    if (!windows && !Files.isExecutable(executable)) {
      throw new IOException(binary + " not executable at " + executable);
    }
    LOG.debug("Resolved {} executable at {}", binary, executable);
    return executable;
  }

  private static String stripArchiveExtension(String name) {
    if (name.toLowerCase(Locale.ENGLISH).endsWith(".tar.gz")) {
      return name.substring(0, name.length() - ".tar.gz".length());
    }
    if (name.toLowerCase(Locale.ENGLISH).endsWith(".tgz")) {
      return name.substring(0, name.length() - ".tgz".length());
    }
    if (name.toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
      return name.substring(0, name.length() - ".zip".length());
    }
    return name;
  }

  private static String fileName(String url) throws MalformedURLException {
    Path fileName = Path.of(new URL(url).getPath()).getFileName();
    if (fileName == null) {
      // SpotBugs: guard against URLs ending with a trailing slash that would trigger an NPE.
      throw new MalformedURLException("URL does not contain a file name: " + url);
    }
    return fileName.toString();
  }
}
