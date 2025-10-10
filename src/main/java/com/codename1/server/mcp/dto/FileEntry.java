package com.codename1.server.mcp.dto;

/** Represents a single in-memory file used for compilation or scaffolding. */
public record FileEntry(String path, String content) {}
