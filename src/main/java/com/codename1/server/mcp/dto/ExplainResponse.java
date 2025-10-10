package com.codename1.server.mcp.dto;

/** Explanation text returned for a requested code issue. */
public record ExplainResponse(String summary, String bad, String good) {}
