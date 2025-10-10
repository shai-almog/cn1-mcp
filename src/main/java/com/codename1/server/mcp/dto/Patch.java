package com.codename1.server.mcp.dto;

/**
 * Patch metadata describing a change to apply to a file.
 *
 * @param description human readable summary of the patch
 * @param diff        unified diff applying the patch
 */
public record Patch(String description, String diff) {}
