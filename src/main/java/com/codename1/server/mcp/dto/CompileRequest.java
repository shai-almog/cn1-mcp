package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Request describing a Java compilation job to run through the external compiler.
 *
 * @param files the source files to compile
 * @param classpathHint optional additional classpath entries supplied by the caller
 */
public record CompileRequest(List<FileEntry> files, String classpathHint) {

  /**
   * Creates a new request while defensively copying the supplied file list.
   *
   * @param files the source files to compile
   * @param classpathHint optional additional classpath entries supplied by the caller
   */
  public CompileRequest {
    // SpotBugs: capture an immutable snapshot of the supplied files list.
    files = files == null ? null : List.copyOf(files);
  }
}
