package com.codename1.server.mcp.dto;

import java.util.List;

public record CompileResponse(boolean ok, String javacOutput, List<CompilerError> errors) {
    public CompileResponse {
        // SpotBugs: expose compiler errors as an immutable list for consumers.
        errors = errors == null ? null : List.copyOf(errors);
    }
}
