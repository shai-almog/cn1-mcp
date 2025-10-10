package com.codename1.server.mcp.dto;

import java.util.List;

/** Request payload describing sources to compile and optional classpath hints. */
public record CompileRequest(List<FileEntry> files, String classpathHint) {
  /**
   * Copies the provided files list to keep the request immutable.
   */
  public CompileRequest {
    // SpotBugs: capture an immutable snapshot of the supplied files list.
    files = files == null ? null : List.copyOf(files);
  }
}
