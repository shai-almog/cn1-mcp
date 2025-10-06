package com.codename1.server.mcp.tools;

import com.codename1.server.mcp.dto.Patch;

public final class PatchUtil {
    private PatchUtil() {}
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