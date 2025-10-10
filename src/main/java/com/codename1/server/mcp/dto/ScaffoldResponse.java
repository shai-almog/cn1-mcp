package com.codename1.server.mcp.dto;

import java.util.List;

/** Response payload containing the generated scaffold file set. */
public record ScaffoldResponse(List<FileEntry> files) {
  /**
   * Copies the generated files so the response remains immutable.
   */
  public ScaffoldResponse {
    // SpotBugs: keep scaffold outputs immutable for API consumers.
    files = files == null ? null : List.copyOf(files);
  }
}
