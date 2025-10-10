package com.codename1.server.mcp.dto;

import java.util.List;

/** Response payload containing the generated native stub source files. */
public record NativeStubResponse(List<FileEntry> files) {
  /**
   * Normalizes the generated files into an immutable list for safe sharing.
   */
  public NativeStubResponse {
    // SpotBugs: respond with an immutable view of generated files.
    files = files == null ? null : List.copyOf(files);
  }
}
