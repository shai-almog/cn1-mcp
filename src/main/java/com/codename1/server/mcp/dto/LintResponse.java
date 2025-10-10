package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Response returned from the lint endpoint summarizing diagnostics and quick fixes.
 *
 * @param ok whether the lint run completed without finding blocking issues
 * @param diagnostics the diagnostics raised during linting
 * @param quickFixes automatically generated quick fix suggestions
 */
public record LintResponse(boolean ok, List<LintDiag> diagnostics, List<QuickFix> quickFixes) {

  /**
   * Creates a new response while defensively copying diagnostics and quick fixes.
   *
   * @param ok whether the lint run completed without finding blocking issues
   * @param diagnostics the diagnostics raised during linting
   * @param quickFixes automatically generated quick fix suggestions
   */
  public LintResponse {
    // SpotBugs: ensure diagnostics and quick fixes remain immutable for callers.
    diagnostics = diagnostics == null ? null : List.copyOf(diagnostics);
    quickFixes = quickFixes == null ? null : List.copyOf(quickFixes);
  }
}
