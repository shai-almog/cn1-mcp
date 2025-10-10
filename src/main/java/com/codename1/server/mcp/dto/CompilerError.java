package com.codename1.server.mcp.dto;

public record CompilerError(String file, int line, int col, String message) {}
