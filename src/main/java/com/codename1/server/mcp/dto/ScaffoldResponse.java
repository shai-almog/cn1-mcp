package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Response containing the generated files for a scaffold request.
 *
 * @param files generated files composing the scaffolded project
 */
public record ScaffoldResponse(List<FileEntry> files) {

  /**
   * Creates a new response while defensively copying the file list.
   *
   * @param files generated files composing the scaffolded project
   */
  public ScaffoldResponse {
    // SpotBugs: keep scaffold outputs immutable for API consumers.
    files = files == null ? null : List.copyOf(files);
  }
}
