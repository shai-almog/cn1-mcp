package com.codename1.server.mcp.dto;

import java.util.List;

public record ScaffoldResponse(List<FileEntry> files) {
    public ScaffoldResponse {
        // SpotBugs: keep scaffold outputs immutable for API consumers.
        files = files == null ? null : List.copyOf(files);
    }
}
