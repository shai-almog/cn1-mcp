package com.codename1.server.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalExtractorTest {

  private Path cacheDir;
  private Map<String, byte[]> resources;
  private TestExtractor extractor;

  @BeforeEach
  void setUp() throws IOException {
    cacheDir = Files.createTempDirectory("cache-");
    resources = new HashMap<>();
    extractor = new TestExtractor(cacheDir.toString(), "v1", resources);
  }

  @Test
  void ensureFileCachesResource() throws Exception {
    resources.put("/cn1.jar", "data".getBytes());

    Path first = extractor.ensureFile("/cn1.jar");
    Path second = extractor.ensureFile("/cn1.jar");

    assertThat(first).exists();
    assertThat(second).isEqualTo(first);
    assertThat(Files.readString(first)).isEqualTo("data");
  }

  @Test
  void ensureArchiveExtractedHandlesZip() throws Exception {
    resources.put("/jdks/jdk.zip", zip(Map.of("bin/javac", "echo")));

    Path root = extractor.ensureArchiveExtracted("/jdks/jdk.zip", "jdk");

    assertThat(root.resolve("bin/javac")).exists();
  }

  @Test
  void ensureArchiveExtractedHandlesTarGz() throws Exception {
    resources.put("/jdks/jdk.tar.gz", tarGz(Map.of("bin/java", "echo")));

    Path root = extractor.ensureArchiveExtracted("/jdks/jdk.tar.gz", "jdk");

    assertThat(root.resolve("bin/java")).exists();
  }

  @Test
  void ensureArchiveExtractedFromUrlUsesFetcher() throws Exception {
    extractor.setUrlBytes(zip(Map.of("bin/tool", "echo")));

    Path root = extractor.ensureArchiveExtractedFromUrl("https://example.com/tool.zip", "tool");

    assertThat(root.resolve("bin/tool")).exists();
  }

  @Test
  void archiveTypeResolutionSupportsKnownExtensions() {
    assertThat(GlobalExtractor.ArchiveType.fromName("jdk.tar.gz"))
        .isEqualTo(GlobalExtractor.ArchiveType.TAR_GZ);
    assertThat(GlobalExtractor.ArchiveType.fromName("jdk.tgz"))
        .isEqualTo(GlobalExtractor.ArchiveType.TAR_GZ);
    assertThat(GlobalExtractor.ArchiveType.fromName("jdk.zip"))
        .isEqualTo(GlobalExtractor.ArchiveType.ZIP);
    assertThatThrownBy(() -> GlobalExtractor.ArchiveType.fromName("jdk.bin"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void zipSlipDetectionPreventsEscape() throws Exception {
    resources.put("/escape.zip", zip(Map.of("../evil", "bad")));

    assertThatThrownBy(() -> extractor.ensureArchiveExtracted("/escape.zip", "escape"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Zip Slip");
  }

  private static byte[] zip(Map<String, String> entries) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      for (var entry : entries.entrySet()) {
        zip.putNextEntry(new ZipEntry(entry.getKey()));
        zip.write(entry.getValue().getBytes());
        zip.closeEntry();
      }
    }
    return out.toByteArray();
  }

  private static byte[] tarGz(Map<String, String> entries) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (GzipCompressorOutputStream gzip = new GzipCompressorOutputStream(out);
        TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
      for (var entry : entries.entrySet()) {
        byte[] data = entry.getValue().getBytes();
        TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getKey());
        tarEntry.setSize(data.length);
        tar.putArchiveEntry(tarEntry);
        tar.write(data);
        tar.closeArchiveEntry();
      }
      tar.finish();
    }
    return out.toByteArray();
  }

  private static final class TestExtractor extends GlobalExtractor {
    private final Map<String, byte[]> resources;
    private byte[] urlBytes;

    private TestExtractor(String cacheDir, String version, Map<String, byte[]> resources) {
      super(cacheDir, version);
      this.resources = resources;
    }

    void setUrlBytes(byte[] bytes) {
      this.urlBytes = bytes;
    }

    @Override
    protected byte[] readResource(String path) throws IOException {
      byte[] data = resources.get(path);
      if (data == null) {
        throw new IOException("missing resource: " + path);
      }
      return data;
    }

    @Override
    protected java.io.InputStream openUrl(String url) {
      return new ByteArrayInputStream(urlBytes);
    }
  }
}
