package com.codename1.server.mcp.dto;

import java.util.List;

public record CompileRequest(List<FileEntry> files, String classpathHint) {
    public CompileRequest {
        // SpotBugs: capture an immutable snapshot of the supplied files list.
        files = files == null ? null : List.copyOf(files);
    }
}
