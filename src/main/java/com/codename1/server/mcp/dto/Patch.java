package com.codename1.server.mcp.dto;

/** Represents a textual diff patch to apply to user code. */
public record Patch(String description, String diff) {}
