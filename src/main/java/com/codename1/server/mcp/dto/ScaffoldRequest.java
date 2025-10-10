package com.codename1.server.mcp.dto;

import java.util.List;

/** Request describing the scaffold project to generate. */
public record ScaffoldRequest(String name, String pkg, List<String> features) {
  /**
   * Copies the optional feature list so downstream processing remains deterministic.
   */
  public ScaffoldRequest {
    // SpotBugs: freeze the feature list to keep scaffold generation deterministic.
    features = features == null ? null : List.copyOf(features);
  }
}
