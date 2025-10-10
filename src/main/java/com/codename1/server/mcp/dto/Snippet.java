package com.codename1.server.mcp.dto;

/**
 * A code snippet that can be suggested or returned by the MCP server.
 *
 * @param title human readable title describing the snippet
 * @param description short description of what the snippet demonstrates
 * @param code the source code for the snippet
 */
public record Snippet(String title, String description, String code) {}
