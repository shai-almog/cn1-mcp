package com.codename1.server.mcp.dto;

import java.util.List;

/**
 * Request describing the project scaffold that should be generated.
 *
 * @param name desired application name
 * @param pkg desired Java package name
 * @param features optional feature identifiers to include in the scaffold
 */
public record ScaffoldRequest(String name, String pkg, List<String> features) {

  /**
   * Creates a new request while defensively copying the feature list.
   *
   * @param name desired application name
   * @param pkg desired Java package name
   * @param features optional feature identifiers to include in the scaffold
   */
  public ScaffoldRequest {
    // SpotBugs: freeze the feature list to keep scaffold generation deterministic.
    features = features == null ? null : List.copyOf(features);
  }
}
