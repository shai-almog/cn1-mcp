package com.codename1.server.mcp.dto;

/** Result of compiling Codename One CSS assets. */
public record CssCompileResponse(boolean ok, String log) {}
