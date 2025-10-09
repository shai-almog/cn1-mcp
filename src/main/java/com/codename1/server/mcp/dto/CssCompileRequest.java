package com.codename1.server.mcp.dto;

import java.util.List;

public record CssCompileRequest(List<FileEntry> files, String inputPath, String outputPath) {
}
