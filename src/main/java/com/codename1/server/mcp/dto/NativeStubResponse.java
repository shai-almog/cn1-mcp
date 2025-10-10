package com.codename1.server.mcp.dto;

import java.util.List;

/** Response payload containing the generated native stub source files. */
public record NativeStubResponse(List<FileEntry> files) {

  /**
   * Creates a new response while defensively copying the generated files list.
   *
   * @param files generated native stub source files
   */
  public NativeStubResponse {
    // SpotBugs: respond with an immutable view of generated files.
    files = files == null ? null : List.copyOf(files);
  }
}
