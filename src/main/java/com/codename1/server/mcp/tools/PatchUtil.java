package com.codename1.server.mcp.tools;

import com.codename1.server.mcp.dto.Patch;

/** Utility helpers for constructing canned patches surfaced by the MCP services. */
public final class PatchUtil {
  private PatchUtil() {}

  /**
   * Produces a patch that wraps UI mutations in a {@code Display.callSerially} block.
   *
   * @return the predefined patch
   */
  public static Patch wrapEdtPatch() {
    String diff =
        """
                @@
                - // UI code here
                + com.codename1.ui.Display.getInstance().callSerially(() -> {
                +   // UI code here
                + });
                """;
    return new Patch("Wrap UI mutation in EDT call", diff);
  }
}
