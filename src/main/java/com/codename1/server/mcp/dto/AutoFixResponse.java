package com.codename1.server.mcp.dto;

import java.util.List;

/** Response returned after applying automated fixes to supplied code. */
public record AutoFixResponse(String patchedCode, List<Patch> patches) {
  /**
   * Copies the generated patch list so that the response remains immutable.
   */
  public AutoFixResponse {
    // SpotBugs: expose a defensive copy of patch metadata to keep responses immutable.
    patches = patches == null ? null : List.copyOf(patches);
  }
}
