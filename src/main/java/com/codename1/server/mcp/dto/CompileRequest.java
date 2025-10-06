package com.codename1.server.mcp.dto;

import java.util.List;

public record CompileRequest(List<FileEntry> files, String classpathHint) {}
