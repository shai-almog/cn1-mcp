package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Result payload returned by the external Java compilation endpoint.
 *
 * @param ok whether compilation completed successfully
 * @param javacOutput the raw output produced by the compiler
 * @param errors structured compiler errors extracted from the run
 */
public record CompileResponse(boolean ok, String javacOutput, List<CompilerError> errors) {

  /**
   * Creates a new response while defensively copying compiler errors.
   *
   * @param ok whether compilation completed successfully
   * @param javacOutput the raw output produced by the compiler
   * @param errors structured compiler errors extracted from the run
   */
  public CompileResponse {
    // SpotBugs: expose compiler errors as an immutable list for consumers.
    errors = errors == null ? null : List.copyOf(errors);
  }
}
