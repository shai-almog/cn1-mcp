package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Request asking the MCP service to automatically repair diagnostics produced by lint.
 *
 * @param code         the source code to analyze and patch
 * @param diagnostics  diagnostics that should be addressed by the auto-fix routine
 */
public record AutoFixRequest(String code, List<LintDiag> diagnostics) {

    /**
     * Creates a new request while defensively copying diagnostics for immutability.
     *
     * @param code        the source code to analyze and patch
     * @param diagnostics diagnostics that should be addressed by the auto-fix routine
     */
    public AutoFixRequest {
        // SpotBugs: return an immutable snapshot so callers cannot mutate our internal state.
        diagnostics = diagnostics == null ? null : List.copyOf(diagnostics);
    }
}
