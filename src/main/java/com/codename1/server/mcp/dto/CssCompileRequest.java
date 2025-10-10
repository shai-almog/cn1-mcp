package com.codename1.server.mcp.dto;

import java.util.List;

/** Request payload for a CSS compilation run. */
public record CssCompileRequest(List<FileEntry> files, String inputPath, String outputPath) {
  /**
   * Normalizes the list of files to an unmodifiable copy to prevent external mutation.
   */
  public CssCompileRequest {
    // SpotBugs: prevent callers from mutating the uploaded file list after construction.
    files = files == null ? null : List.copyOf(files);
  }
}
