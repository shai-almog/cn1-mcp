package com.codename1.server.mcp.dto;

import java.util.List;

public record AutoFixRequest(String code, List<LintDiag> diagnostics) {
    public AutoFixRequest {
        // SpotBugs: return an immutable snapshot so callers cannot mutate our internal state.
        diagnostics = diagnostics == null ? null : List.copyOf(diagnostics);
    }
}
