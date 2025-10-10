package com.codename1.server.mcp.dto;

import java.util.List;

/** Request payload describing diagnostics to auto-fix for a given source snippet. */
public record AutoFixRequest(String code, List<LintDiag> diagnostics) {
  /**
   * Copies the diagnostics to avoid subsequent external mutations.
   */
  public AutoFixRequest {
    // SpotBugs: return an immutable snapshot so callers cannot mutate our internal state.
    diagnostics = diagnostics == null ? null : List.copyOf(diagnostics);
  }
}
