package com.codename1.server.mcp.dto;

import java.util.List;

public record CompileResponse(boolean ok, String javacOutput, List<CompilerError> errors) {}
