package com.codename1.server.mcp.dto;

import java.util.List;

/** Result produced by the linting pipeline, including diagnostics and quick fixes. */
public record LintResponse(boolean ok, List<LintDiag> diagnostics, List<QuickFix> quickFixes) {
  /**
   * Copies diagnostics and quick fixes to ensure immutability of the response payload.
   */
  public LintResponse {
    // SpotBugs: ensure diagnostics and quick fixes remain immutable for callers.
    diagnostics = diagnostics == null ? null : List.copyOf(diagnostics);
    quickFixes = quickFixes == null ? null : List.copyOf(quickFixes);
  }
}
