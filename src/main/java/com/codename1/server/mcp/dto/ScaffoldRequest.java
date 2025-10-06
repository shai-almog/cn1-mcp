package com.codename1.server.mcp.dto;

import java.util.List;

public record ScaffoldRequest(String name, String pkg, List<String> features) {}
