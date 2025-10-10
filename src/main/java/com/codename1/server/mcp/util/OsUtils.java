package com.codename1.server.mcp.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Utility methods for interacting with the host operating system. */
public final class OsUtils {
  private OsUtils() {}

  /** Returns {@code true} if the current JVM is running on a Linux-based OS. */
  public static boolean isLinux() {
    return osName().contains("linux");
  }

  /** Returns {@code true} if the current JVM is running on Windows. */
  public static boolean isWindows() {
    return osName().contains("win");
  }

  /** Returns {@code true} if the current JVM is running on macOS. */
  public static boolean isMac() {
    String name = osName();
    return name.contains("mac") || name.contains("darwin");
  }

  /**
   * Locates the given binary on the {@code PATH} and returns an executable {@link Path}, or
   * {@code null} if the command is unavailable.
   */
  public static Path locateOnPath(String binary) {
    String path = System.getenv("PATH");
    if (path == null || path.isBlank()) {
      return null;
    }
    String separator = System.getProperty("path.separator");
    for (String segment : path.split(separator)) {
      if (segment == null || segment.isBlank()) {
        continue;
      }
      Path candidate = Path.of(segment).resolve(binary);
      if (Files.isExecutable(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  private static String osName() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
  }
}
