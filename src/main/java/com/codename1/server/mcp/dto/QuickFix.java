package com.codename1.server.mcp.dto;

/**
 * Quick fix suggestion returned by the lint service.
 *
 * @param title human readable description of the fix
 * @param patch patch to apply in order to fix the issue
 */
public record QuickFix(String title, Patch patch) {}
