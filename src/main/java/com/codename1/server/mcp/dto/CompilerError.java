package com.codename1.server.mcp.dto;

/**
 * Structured compiler error extracted from a javac invocation.
 *
 * @param file    the file that triggered the error
 * @param line    the line number associated with the error
 * @param col     the column number associated with the error
 * @param message the textual description reported by javac
 */
public record CompilerError(String file, int line, int col, String message) {}
