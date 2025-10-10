package com.codename1.server.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class OsUtilsTest {

  private String originalOsName;

  @BeforeEach
  void setUp() {
    originalOsName = System.getProperty("os.name");
  }

  @AfterEach
  void tearDown() {
    if (originalOsName != null) {
      System.setProperty("os.name", originalOsName);
    }
  }

  @Test
  void detectsOperatingSystems() {
    System.setProperty("os.name", "Linux");
    assertThat(OsUtils.isLinux()).isTrue();
    assertThat(OsUtils.isWindows()).isFalse();
    assertThat(OsUtils.isMac()).isFalse();

    System.setProperty("os.name", "Windows 11");
    assertThat(OsUtils.isWindows()).isTrue();

    System.setProperty("os.name", "Mac OS X");
    assertThat(OsUtils.isMac()).isTrue();
  }

  @Test
  void locateOnPathHandlesMissingCommands() {
    String missing = "definitely-missing-" + System.nanoTime();
    assertThat(OsUtils.locateOnPath(missing)).isNull();
  }

  @Test
  void locateOnPathDetectsExistingCommandsWhenAvailable() {
    Path shell = OsUtils.locateOnPath("sh");
    Assumptions.assumeTrue(shell != null, "Shell not on PATH");
    assertThat(shell).exists();
  }
}
