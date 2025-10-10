package com.codename1.server.mcp.dto;

/** Represents a compiler error emitted while building project sources. */
public record CompilerError(String file, int line, int col, String message) {}
