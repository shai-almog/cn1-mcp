package com.codename1.server.mcp.dto;

import java.util.List;

public record LintRequest(String code,
                          String language,
                          List<String> ruleset) {
    public LintRequest {
        // SpotBugs: snapshot the requested ruleset so later mutations do not affect lint execution.
        ruleset = ruleset == null ? null : List.copyOf(ruleset);
    }
}

