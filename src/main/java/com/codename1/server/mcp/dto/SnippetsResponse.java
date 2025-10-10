package com.codename1.server.mcp.dto;

import java.util.List;

public record SnippetsResponse(List<Snippet> snippets) {
    public SnippetsResponse {
        // SpotBugs: share an immutable view of snippet results.
        snippets = snippets == null ? null : List.copyOf(snippets);
    }
}
