package com.codename1.server.mcp.tools;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Extracts binary archives and resources into a shared cache with process-safe locking. */
public class GlobalExtractor {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExtractor.class);
  private static final ConcurrentHashMap<String, ReentrantLock> LOCAL_LOCKS =
      new ConcurrentHashMap<>();
  private static final Method ZIP_GET_UNIX_MODE = resolveMethod("getUnixMode");
  private static final Method ZIP_GET_EXTERNAL_ATTRIBUTES = resolveMethod("getExternalAttributes");

  private final Path cacheDir;
  private final String versionTag;

  /**
   * Creates a new extractor rooted at the given cache directory.
   *
   * @param cacheDir the directory used to store cached artifacts
   * @param versionTag a tag appended to cache directories to prevent collisions
   */
  public GlobalExtractor(String cacheDir, String versionTag) {
    this.cacheDir = Paths.get(Objects.requireNonNull(cacheDir));
    this.versionTag = Objects.requireNonNull(versionTag);
  }

  /** Make overridable for tests. */
  protected byte[] readResource(String path) throws IOException {
    try (InputStream in = GlobalExtractor.class.getResourceAsStream(path)) {
      if (in == null) {
        throw new FileNotFoundException("Resource not found: " + path);
      }
      return in.readAllBytes();
    }
  }

  /**
   * Ensures the specified classpath resource is cached on disk.
   *
   * @param resourcePath the classpath resource path
   * @return the cached file path
   */
  public Path ensureFile(String resourcePath) throws IOException {
    byte[] bytes = readResource(resourcePath);
    String hash = sha256(bytes).substring(0, 12);

    Path base = cacheDir.resolve("libs").resolve(versionTag + "-" + hash);
    Files.createDirectories(base);

    Path fileName = Path.of(resourcePath).getFileName();
    if (fileName == null) {
      throw new IOException("Resource path has no filename: " + resourcePath);
    }
    Path out = base.resolve(fileName.toString());
    if (Files.exists(out)) {
      LOG.debug("Resource {} already extracted at {}", resourcePath, out);
      return out;
    }

    Path lockPath = base.resolve(".extract.lock");
    ReentrantLock local =
        LOCAL_LOCKS.computeIfAbsent(lockPath.toString(), key -> new ReentrantLock());

    local.lock();
    try (FileChannel channel =
            FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock ignored = channel.lock()) {
      // OS-level lock for cross-process safety.
      if (!Files.exists(out)) {
        LOG.info("Extracting resource {} to {}", resourcePath, out);
        Path tmp = Files.createTempFile(base, ".res", ".tmp");
        Files.write(tmp, bytes, StandardOpenOption.TRUNCATE_EXISTING);
        Files.move(tmp, out, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      local.unlock();
    }
    return out;
  }

  /**
   * Ensures the given classpath archive resource is extracted into the cache.
   *
   * @param archiveResourcePath the classpath resource representing the archive
   * @param folderName the target directory name within the cache
   * @return the extracted archive root
   */
  public Path ensureArchiveExtracted(String archiveResourcePath, String folderName)
      throws IOException {
    byte[] bytes = readResource(archiveResourcePath);
    ArchiveType type = ArchiveType.fromName(archiveResourcePath);
    String identifier = archiveResourcePath + ":" + sha256(bytes);
    return ensureArchiveExtracted(
        identifier, type, () -> new ByteArrayInputStream(bytes), folderName);
  }

  private Path ensureArchiveExtracted(
      String identifier, ArchiveType type, IoSupplier<InputStream> supplier, String folderName)
      throws IOException {
    String hash = sha256(identifier).substring(0, 12);

    Path parent = cacheDir.resolve("jdks").resolve(versionTag + "-" + hash);
    Path destRoot = parent.resolve(folderName);
    if (Files.exists(destRoot)) {
      LOG.debug("Archive {} already extracted at {}", identifier, destRoot);
      return destRoot;
    }

    Path lockPath = parent.resolve(".extract.lock");
    ReentrantLock local =
        LOCAL_LOCKS.computeIfAbsent(lockPath.toString(), key -> new ReentrantLock());

    local.lock();
    try {
      Files.createDirectories(parent);
      try (FileChannel channel =
              FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
          FileLock ignored = channel.lock()) {
        if (Files.exists(destRoot)) {
          return destRoot;
        }

        Path archivePath = parent.resolve("archive" + type.extension);
        if (!Files.exists(archivePath)) {
          Path tmpArchive = Files.createTempFile(parent, "download-", type.extension);
          try (InputStream in = supplier.get();
              OutputStream out =
                  Files.newOutputStream(tmpArchive, StandardOpenOption.TRUNCATE_EXISTING)) {
            if (in == null) {
              throw new IOException("Archive supplier returned null stream for " + identifier);
            }
            in.transferTo(out);
          }
          Files.move(
              tmpArchive,
              archivePath,
              StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
          LOG.info("Fetched archive {} into {}", identifier, archivePath);
        }

        Path tmpRoot = Files.createTempDirectory(parent, "tmp-extract-");
        boolean success = false;
        try {
          extractArchive(archivePath, type, tmpRoot);
          Files.move(tmpRoot, destRoot, StandardCopyOption.ATOMIC_MOVE);
          LOG.info("Extracted archive {} to {}", identifier, destRoot);
          success = true;
        } finally {
          if (!success) {
            cleanupDirectory(tmpRoot);
          }
        }
      }
    } finally {
      local.unlock();
    }
    return destRoot;
  }

  /**
   * Ensures the given remote archive is downloaded and extracted into the cache.
   *
   * @param url the URL pointing to the archive to download
   * @param folderName the target directory name within the cache
   * @return the extracted archive root
   */
  public Path ensureArchiveExtractedFromUrl(String url, String folderName) throws IOException {
    ArchiveType type = ArchiveType.fromName(url);
    return ensureArchiveExtracted("url:" + url, type, () -> openUrl(url), folderName);
  }

  private void extractArchive(Path archivePath, ArchiveType type, Path destRoot)
      throws IOException {
    switch (type) {
      case TAR_GZ -> extractTarGz(archivePath, destRoot);
      case ZIP -> extractZip(archivePath, destRoot);
      default -> throw new IllegalArgumentException("Unsupported archive type: " + type);
    }
  }

  private void extractTarGz(Path archivePath, Path destRoot) throws IOException {
    try (InputStream in = Files.newInputStream(archivePath);
        GzipCompressorInputStream gzi = new GzipCompressorInputStream(in);
        TarArchiveInputStream tar = new TarArchiveInputStream(gzi)) {

      TarArchiveEntry e;
      while ((e = tar.getNextTarEntry()) != null) {
        Path out = destRoot.resolve(e.getName()).normalize();
        if (!out.startsWith(destRoot)) {
          throw new IOException("Zip Slip detected: " + out);
        }
        if (e.isDirectory()) {
          Files.createDirectories(out);
        } else {
          Path parent = out.getParent();
          if (parent != null) {
            Files.createDirectories(parent);
          }
          try (OutputStream os = Files.newOutputStream(out)) {
            tar.transferTo(os);
          }
          if ((e.getMode() & 0x40) != 0) {
            out.toFile().setExecutable(true);
          }
        }
      }
    }
  }

  private void extractZip(Path archivePath, Path destRoot) throws IOException {
    try (InputStream in = Files.newInputStream(archivePath);
        ZipInputStream zip = new ZipInputStream(in)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        Path out = destRoot.resolve(entry.getName()).normalize();
        if (!out.startsWith(destRoot)) {
          throw new IOException("Zip Slip detected: " + out);
        }
        if (entry.isDirectory()) {
          Files.createDirectories(out);
        } else {
          Path parent = out.getParent();
          if (parent != null) {
            Files.createDirectories(parent);
          }
          try (OutputStream os = Files.newOutputStream(out)) {
            zip.transferTo(os);
          }
          if (isExecutable(entry)) {
            out.toFile().setExecutable(true);
          }
        }
      }
    }
  }

  private static Method resolveMethod(String name) {
    try {
      return ZipEntry.class.getMethod(name);
    } catch (NoSuchMethodException e) {
      LOG.debug("ZipEntry method {} not available on this JDK", name);
      return null;
    }
  }

  private static boolean isExecutable(ZipEntry entry) {
    try {
      if (ZIP_GET_UNIX_MODE != null) {
        int unixMode = (int) ZIP_GET_UNIX_MODE.invoke(entry);
        if (unixMode != -1) {
          return (unixMode & 0x40) != 0;
        }
      }
      if (ZIP_GET_EXTERNAL_ATTRIBUTES != null) {
        long attrs = (long) ZIP_GET_EXTERNAL_ATTRIBUTES.invoke(entry);
        return ((attrs >> 16) & 0x40) != 0;
      }
    } catch (ReflectiveOperationException e) {
      LOG.debug("Failed to inspect permissions for zip entry {}", entry.getName(), e);
    }
    return false;
  }

  private void cleanupDirectory(Path dir) {
    if (dir == null || !Files.exists(dir)) {
      return;
    }
    try {
      Files.walk(dir)
          .sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException ex) {
                  // SpotBugs: log cleanup failures; cleanup remains best-effort.
                  LOG.debug("Failed to delete cached artifact {}", path, ex);
                }
              });
    } catch (IOException ex) {
      LOG.debug("Failed to walk cache directory {}", dir, ex);
    }
  }

  protected InputStream openUrl(String url) throws IOException {
    return new URL(url).openStream();
  }

  /** Supported archive formats that can be extracted into the local cache. */
  public enum ArchiveType {
    TAR_GZ(".tar.gz"),
    ZIP(".zip");

    private final String extension;

    ArchiveType(String extension) {
      this.extension = extension;
    }

    static ArchiveType fromName(String name) {
      String lower = name.toLowerCase(Locale.ROOT);
      if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
        return TAR_GZ;
      }
      if (lower.endsWith(".zip")) {
        return ZIP;
      }
      throw new IllegalArgumentException("Unsupported archive type: " + name);
    }
  }

  @FunctionalInterface
  private interface IoSupplier<T> {
    T get() throws IOException;
  }

  private static String sha256(String s) {
    return sha256(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private static String sha256(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(data));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
