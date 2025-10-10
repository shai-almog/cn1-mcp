package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Response describing the outcome of an auto-fix request.
 *
 * @param patchedCode the transformed source code returned to the caller
 * @param patches machine-readable patch entries that were applied
 */
public record AutoFixResponse(String patchedCode, List<Patch> patches) {

  /**
   * Creates a new response while defensively copying patch metadata.
   *
   * @param patchedCode the transformed source code returned to the caller
   * @param patches machine-readable patch entries that were applied
   */
  public AutoFixResponse {
    // SpotBugs: expose a defensive copy of patch metadata to keep responses immutable.
    patches = patches == null ? null : List.copyOf(patches);
  }
}
