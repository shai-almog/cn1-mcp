package com.codename1.server.mcp.dto;

/**
 * Represents a range within a source file.
 *
 * @param start the inclusive start position
 * @param end the inclusive end position
 */
public record Range(Pos start, Pos end) {

  /**
   * Position within a file using one-based line and column numbers.
   *
   * @param line the one-based line number
   * @param col the one-based column number
   */
  public record Pos(int line, int col) {}
}
