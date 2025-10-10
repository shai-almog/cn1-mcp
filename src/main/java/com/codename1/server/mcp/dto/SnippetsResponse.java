package com.codename1.server.mcp.dto;

import java.util.List;

/** Response containing snippets that match a request topic. */
public record SnippetsResponse(List<Snippet> snippets) {
  /**
   * Copies the snippet list to keep the response immutable.
   */
  public SnippetsResponse {
    // SpotBugs: share an immutable view of snippet results.
    snippets = snippets == null ? null : List.copyOf(snippets);
  }
}
