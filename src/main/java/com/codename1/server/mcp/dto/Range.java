package com.codename1.server.mcp.dto;

/** Represents a range within a text document, inclusive of both endpoints. */
public record Range(Pos start, Pos end) {
  /** Point in a document identified by line and column. */
  public record Pos(int line, int col) {}
}
