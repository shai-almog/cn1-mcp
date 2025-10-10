package com.codename1.server.mcp.dto;

import java.util.List;

/** Request payload describing the code to lint and rule configuration. */
public record LintRequest(
    String code,
    String language,
    List<String> ruleset) {
  /**
   * Copies the optional rule set list to guard against external mutation after construction.
   */
  public LintRequest {
    // SpotBugs: snapshot the requested ruleset so later mutations do not affect lint execution.
    ruleset = ruleset == null ? null : List.copyOf(ruleset);
  }
}
