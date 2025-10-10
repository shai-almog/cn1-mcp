package com.codename1.server.mcp.dto;

public record LintDiag(String ruleId, String severity, String message, Range range) {}
