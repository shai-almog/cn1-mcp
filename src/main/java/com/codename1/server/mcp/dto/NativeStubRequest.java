package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Request payload for the native stub generation tool. The caller supplies the
 * Java source files that make up the compilation unit (at minimum the native
 * interface itself) along with the fully-qualified interface name that should
 * be processed.
 */
public record NativeStubRequest(List<FileEntry> files, String interfaceName) {

    /**
     * Creates a new request while defensively copying the provided source files.
     *
     * @param files         Java source files that comprise the compilation unit
     * @param interfaceName the fully qualified interface name to process
     */
    public NativeStubRequest {
        // SpotBugs: retain an immutable snapshot of provided source files.
        files = files == null ? null : List.copyOf(files);
    }
}
