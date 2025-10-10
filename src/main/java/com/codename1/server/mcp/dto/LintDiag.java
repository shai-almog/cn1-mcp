package com.codename1.server.mcp.dto;

/**
 * Diagnostic produced during linting.
 *
 * @param ruleId   the identifier of the violated rule
 * @param severity severity level for the diagnostic
 * @param message  human readable description of the issue
 * @param range    location range in the source code
 */
public record LintDiag(String ruleId, String severity, String message, Range range) {}
