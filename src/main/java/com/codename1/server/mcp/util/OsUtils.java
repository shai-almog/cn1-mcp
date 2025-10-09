package com.codename1.server.mcp.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class OsUtils {
    private OsUtils() {
    }

    public static boolean isLinux() {
        return osName().contains("linux");
    }

    public static boolean isWindows() {
        return osName().contains("win");
    }

    public static boolean isMac() {
        String name = osName();
        return name.contains("mac") || name.contains("darwin");
    }

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
