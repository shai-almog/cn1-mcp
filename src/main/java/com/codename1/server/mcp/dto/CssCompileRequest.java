package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Request payload describing a CSS compilation job.
 *
 * @param files the uploaded input files that should be processed
 * @param inputPath the main CSS file to compile
 * @param outputPath the desired output path for the compiled CSS
 */
public record CssCompileRequest(List<FileEntry> files, String inputPath, String outputPath) {

  /**
   * Creates a new request while defensively copying the uploaded files list.
   *
   * @param files the uploaded input files that should be processed
   * @param inputPath the main CSS file to compile
   * @param outputPath the desired output path for the compiled CSS
   */
  public CssCompileRequest {
    // SpotBugs: prevent callers from mutating the uploaded file list after construction.
    files = files == null ? null : List.copyOf(files);
  }
}
