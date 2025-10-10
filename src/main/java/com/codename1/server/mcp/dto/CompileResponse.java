package com.codename1.server.mcp.dto;

import java.util.List;

/** Response returned from the Java compilation endpoint. */
public record CompileResponse(boolean ok, String javacOutput, List<CompilerError> errors) {
  /**
   * Copies the compiler error list so that callers cannot mutate the response state.
   */
  public CompileResponse {
    // SpotBugs: expose compiler errors as an immutable list for consumers.
    errors = errors == null ? null : List.copyOf(errors);
  }
}
