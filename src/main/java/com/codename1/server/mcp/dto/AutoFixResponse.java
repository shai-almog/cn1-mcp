package com.codename1.server.mcp.dto;

import java.util.List;

public record AutoFixResponse(String patchedCode, List<Patch> patches) {
    public AutoFixResponse {
        // SpotBugs: expose a defensive copy of patch metadata to keep responses immutable.
        patches = patches == null ? null : List.copyOf(patches);
    }
}
