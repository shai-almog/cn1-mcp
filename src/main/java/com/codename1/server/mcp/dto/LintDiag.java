package com.codename1.server.mcp.dto;

/** Describes a single lint diagnostic entry. */
public record LintDiag(String ruleId, String severity, String message, Range range) {}
