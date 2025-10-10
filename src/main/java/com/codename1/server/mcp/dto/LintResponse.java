package com.codename1.server.mcp.dto;

import java.util.List;

public record LintResponse(boolean ok, List<LintDiag> diagnostics, List<QuickFix> quickFixes) {
    public LintResponse {
        // SpotBugs: ensure diagnostics and quick fixes remain immutable for callers.
        diagnostics = diagnostics == null ? null : List.copyOf(diagnostics);
        quickFixes = quickFixes == null ? null : List.copyOf(quickFixes);
    }
}
