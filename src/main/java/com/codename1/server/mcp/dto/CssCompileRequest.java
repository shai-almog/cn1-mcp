package com.codename1.server.mcp.dto;

import java.util.List;

public record CssCompileRequest(List<FileEntry> files, String inputPath, String outputPath) {
    public CssCompileRequest {
        // SpotBugs: prevent callers from mutating the uploaded file list after construction.
        files = files == null ? null : List.copyOf(files);
    }
}
