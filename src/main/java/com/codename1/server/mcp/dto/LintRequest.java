package com.codename1.server.mcp.dto;

import java.util.List;

public record LintRequest(String code,
                          String language,
                          List<String> ruleset) {
}

