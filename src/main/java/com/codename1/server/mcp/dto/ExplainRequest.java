package com.codename1.server.mcp.dto;

/**
 * Request describing which lint rule should be explained.
 *
 * @param ruleId the identifier of the rule to explain
 */
public record ExplainRequest(String ruleId) {}
