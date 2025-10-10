package com.codename1.server.mcp.dto;

/**
 * Result of compiling a CSS file via the MCP service.
 *
 * @param ok whether the compilation completed successfully
 * @param log textual output from the compilation command
 */
public record CssCompileResponse(boolean ok, String log) {}
