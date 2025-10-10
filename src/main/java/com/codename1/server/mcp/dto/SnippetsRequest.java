package com.codename1.server.mcp.dto;

/**
 * Request describing which snippet topic should be retrieved.
 *
 * @param topic topic identifier used to filter snippets
 */
public record SnippetsRequest(String topic) {}
