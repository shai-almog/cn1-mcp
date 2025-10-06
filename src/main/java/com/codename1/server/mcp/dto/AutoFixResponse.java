package com.codename1.server.mcp.dto;

import java.util.List;

public record AutoFixResponse(String patchedCode, List<Patch> patches) {}
