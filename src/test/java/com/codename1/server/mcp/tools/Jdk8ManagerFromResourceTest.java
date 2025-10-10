package com.codename1.server.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Jdk8ManagerFromResourceTest {

  private String originalOs;
  private GlobalExtractor extractor;

  @BeforeEach
  void setUp() {
    originalOs = System.getProperty("os.name");
    extractor = mock(GlobalExtractor.class);
  }

  @AfterEach
  void tearDown() {
    if (originalOs != null) {
      System.setProperty("os.name", originalOs);
    }
  }

  @Test
  void ensureJavacFromLinuxResource() throws Exception {
    System.setProperty("os.name", "Linux");
    Path root = createJdkLayout("javac", false);
    doReturn(root).when(extractor).ensureArchiveExtracted("/jdks/jdk.tar.gz", "jdk");

    Jdk8ManagerFromResource manager =
        new Jdk8ManagerFromResource(extractor, "/jdks/jdk.tar.gz", "release");

    assertThat(manager.ensureJavac8()).isEqualTo(root.resolve("bin").resolve("javac"));
    verify(extractor).ensureArchiveExtracted("/jdks/jdk.tar.gz", "jdk");
  }

  @Test
  void ensureJavaFromWindowsDownload() throws Exception {
    System.setProperty("os.name", "Windows 11");
    Path root = createJdkLayout("java.exe", true);
    doReturn(root)
        .when(extractor)
        .ensureArchiveExtractedFromUrl("https://example.com/jdk.zip", "jdk");

    Jdk8ManagerFromResource manager =
        new Jdk8ManagerFromResource(
            extractor, "/ignored.tar.gz", "https://example.com/mac.zip", "https://example.com/jdk.zip", "release");

    assertThat(manager.ensureJava8()).isEqualTo(root.resolve("bin").resolve("java.exe"));
    verify(extractor).ensureArchiveExtractedFromUrl("https://example.com/jdk.zip", "jdk");
  }

  @Test
  void ensureBinaryValidatesResourcePath() {
    System.setProperty("os.name", "Linux");
    Jdk8ManagerFromResource manager =
        new Jdk8ManagerFromResource(extractor, "", "release");

    assertThatThrownBy(manager::ensureJavac8)
        .isInstanceOf(IOException.class)
        .hasMessageContaining("No bundled JDK8 resource configured");
  }

  @Test
  void ensureBinaryRequiresConfiguredUrl() {
    System.setProperty("os.name", "Windows 11");
    Jdk8ManagerFromResource manager =
        new Jdk8ManagerFromResource(extractor, "/linux.tar.gz", "release");

    assertThatThrownBy(manager::ensureJava8)
        .isInstanceOf(IOException.class)
        .hasMessageContaining("No JDK8 download URL");
  }

  @Test
  void ensureBinarySearchesNestedRoot() throws Exception {
    System.setProperty("os.name", "Linux");
    Path base = Files.createTempDirectory("jdk-nested-");
    Path nestedRoot = base.resolve("deep").resolve("jdk");
    Files.createDirectories(nestedRoot.resolve("bin"));
    Path javac = nestedRoot.resolve("bin").resolve("javac");
    Files.writeString(javac, "echo");
    javac.toFile().setExecutable(true);
    Files.writeString(nestedRoot.resolve("release"), "marker");

    doReturn(base).when(extractor).ensureArchiveExtracted(anyString(), anyString());

    Jdk8ManagerFromResource manager =
        new Jdk8ManagerFromResource(extractor, "/linux.tar.gz", "release");

    assertThat(manager.ensureJavac8()).isEqualTo(javac);
  }

  private static Path createJdkLayout(String binaryName, boolean windows) throws IOException {
    Path root = Files.createTempDirectory("jdk-");
    Files.writeString(root.resolve("release"), "marker");
    Path binDir = root.resolve("bin");
    Files.createDirectories(binDir);
    Path binary = binDir.resolve(binaryName);
    Files.writeString(binary, "echo");
    if (!windows) {
      binary.toFile().setExecutable(true);
    }
    return root;
  }
}
