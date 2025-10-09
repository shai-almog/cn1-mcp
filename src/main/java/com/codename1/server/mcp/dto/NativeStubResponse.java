package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Response payload containing the generated native stub source files.
 */
public record NativeStubResponse(List<FileEntry> files) {
}
