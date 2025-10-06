package com.codename1.server.mcp.dto;

import java.util.List;

public record LintResponse(boolean ok, List<LintDiag> diagnostics, List<QuickFix> quickFixes) {}
