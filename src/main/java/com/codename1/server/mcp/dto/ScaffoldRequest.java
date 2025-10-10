package com.codename1.server.mcp.dto;

import java.util.List;

public record ScaffoldRequest(String name, String pkg, List<String> features) {
    public ScaffoldRequest {
        // SpotBugs: freeze the feature list to keep scaffold generation deterministic.
        features = features == null ? null : List.copyOf(features);
    }
}
