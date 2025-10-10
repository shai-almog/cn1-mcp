package com.codename1.server.mcp.dto;

/**
 * Response providing documentation around a lint rule, including bad and good examples.
 *
 * @param summary overview text for the rule
 * @param bad     example demonstrating a violation
 * @param good    example demonstrating compliant code
 */
public record ExplainResponse(String summary, String bad, String good) {}
