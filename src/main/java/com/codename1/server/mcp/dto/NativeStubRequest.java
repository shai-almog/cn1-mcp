package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Request payload for the native stub generation tool. The caller supplies the
 * Java source files that make up the compilation unit (at minimum the native
 * interface itself) along with the fully-qualified interface name that should
 * be processed.
 */
public record NativeStubRequest(List<FileEntry> files, String interfaceName) {
    public NativeStubRequest {
        // SpotBugs: retain an immutable snapshot of provided source files.
        files = files == null ? null : List.copyOf(files);
    }
}
