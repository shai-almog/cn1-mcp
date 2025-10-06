package com.codename1.server.mcp.dto;

import java.util.List;

public record AutoFixRequest(String code, List<LintDiag> diagnostics) {}
