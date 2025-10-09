package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.CssCompileRequest;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.tools.GlobalExtractor;
import com.codename1.server.mcp.tools.Jdk8ManagerFromResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.codename1.server.mcp.util.OsUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class CssCompileIntegrationTest {
    private static final Path SHARED_CACHE = Paths.get(System.getProperty("java.io.tmpdir"), "cn1-mcp-it-css");
    private static final String LINUX_RESOURCE = "/cn1libs/OpenJDK8U-jdk_x64_linux_hotspot_8u462b08.tar.gz";
    private static final String MAC_URL = "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u382-b05/OpenJDK8U-jdk_x64_mac_hotspot_8u382b05.tar.gz";
    private static final String WINDOWS_URL = "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u382-b05/OpenJDK8U-jdk_x64_windows_hotspot_8u382b05.zip";

    Path cacheDir;
    GlobalExtractor extractor;
    Jdk8ManagerFromResource jdkMgr;

    @BeforeAll
    static void ensureSharedCache() throws Exception {
        Files.createDirectories(SHARED_CACHE);
    }

    @BeforeEach
    void setup() throws Exception {
        cacheDir = SHARED_CACHE.resolve(safeOsName());
        Files.createDirectories(cacheDir);
        extractor = new GlobalExtractor(cacheDir.toString(), "css-it-v1");
        assumeTrue(resourceExists("/cn1libs/designer.jar"), "designer.jar resource missing");
        jdkMgr = new Jdk8ManagerFromResource(extractor, LINUX_RESOURCE, MAC_URL, WINDOWS_URL, "release");
        if (OsUtils.isLinux()) {
            assumeTrue(OsUtils.locateOnPath("xvfb-run") != null, "xvfb-run must be available on Linux test hosts");
        }
    }

    @Test
    void compilesValidCssWithResources() {
        CssCompileService svc = new CssCompileService(extractor, jdkMgr);
        String css = """
                @font-face {
                  font-family: 'Stub';
                  src: url('res/StubFont.ttf');
                }
                .button {
                  background-image: url('res/bg.png');
                  border-radius: 3px;
                }
                """;
        var req = new CssCompileRequest(List.of(new FileEntry("theme.css", css)), "theme.css", "theme.res");
        var res = svc.compile(req);
        assertTrue(res.ok(), () -> "CSS compile failed: \n" + res.log());
        assertTrue(res.log().contains("Using stateless mode"));
    }

    @Test
    void reportsUnsupportedButValidCss() {
        CssCompileService svc = new CssCompileService(extractor, jdkMgr);
        String css = """
                .container {
                  display: grid;
                  grid-template-columns: 1fr 1fr;
                }
                """;
        var req = new CssCompileRequest(List.of(new FileEntry("theme.css", css)), "theme.css", "theme.res");
        var res = svc.compile(req);
        assertTrue(res.ok(), () -> "CSS compile should succeed even if unsupported: \n" + res.log());
        assertTrue(res.log().toLowerCase().contains("unsupported"), () -> "Expected unsupported warning in log: \n" + res.log());
    }

    private static boolean resourceExists(String path) {
        try (InputStream in = CssCompileIntegrationTest.class.getResourceAsStream(path)) {
            return in != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static String safeOsName() {
        return System.getProperty("os.name", "generic").replaceAll("[^A-Za-z0-9]+", "-").toLowerCase();
    }
}
