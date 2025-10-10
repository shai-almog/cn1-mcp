package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Request payload used to run the lint ruleset against a code sample.
 *
 * @param code the source code to lint
 * @param language the language identifier for the lint request
 * @param ruleset optional custom rule identifiers provided by the caller
 */
public record LintRequest(String code, String language, List<String> ruleset) {

  /**
   * Creates a new lint request while defensively copying the ruleset list.
   *
   * @param code the source code to lint
   * @param language the language identifier for the lint request
   * @param ruleset optional custom rule identifiers provided by the caller
   */
  public LintRequest {
    // SpotBugs: snapshot the requested ruleset so later mutations do not affect lint execution.
    ruleset = ruleset == null ? null : List.copyOf(ruleset);
  }
}
