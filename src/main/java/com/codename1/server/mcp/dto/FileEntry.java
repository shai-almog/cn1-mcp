package com.codename1.server.mcp.dto;

/**
 * File entry used to pass source or resource content to the MCP services.
 *
 * @param path path to associate with the file
 * @param content textual contents of the file
 */
public record FileEntry(String path, String content) {}
