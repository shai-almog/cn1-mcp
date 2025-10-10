package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Response containing the snippets that match a given topic.
 *
 * @param snippets the snippets returned for the request
 */
public record SnippetsResponse(List<Snippet> snippets) {

    /**
     * Creates a new response while defensively copying the snippet list.
     *
     * @param snippets the snippets returned for the request
     */
    public SnippetsResponse {
        // SpotBugs: share an immutable view of snippet results.
        snippets = snippets == null ? null : List.copyOf(snippets);
    }
}
