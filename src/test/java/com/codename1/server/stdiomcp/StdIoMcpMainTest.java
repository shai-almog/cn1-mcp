package com.codename1.server.stdiomcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StdIoMcpMainTest {

    @Test
    void handlesInitializeToolListAndLintCall() throws Exception {
        Path cacheDir = Files.createTempDirectory("cn1-mcp-test-cache");
        String requests = String.join("\n", List.of(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}",
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}",
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"cn1_lint_code\",\"arguments\":{\"code\":\"public class Test {}\"}}}"
        )) + "\n";

        var in = new ByteArrayInputStream(requests.getBytes(StandardCharsets.UTF_8));
        var out = new ByteArrayOutputStream();

        StdIoMcpMain.runWithStreams(in, out, new String[]{"--cn1.cacheDir=" + cacheDir.toAbsolutePath()});

        String[] lines = out.toString(StandardCharsets.UTF_8).trim().split("\\R");
        assertEquals(3, lines.length, "Expected three JSON-RPC responses");

        ObjectMapper mapper = new ObjectMapper();

        Map<?,?> init = mapper.readValue(lines[0], Map.class);
        assertEquals("2.0", init.get("jsonrpc"));
        Map<?,?> initResult = (Map<?,?>) init.get("result");
        Map<?,?> serverInfo = (Map<?,?>) initResult.get("serverInfo");
        assertEquals("cn1-mcp", serverInfo.get("name"));

        Map<?,?> tools = mapper.readValue(lines[1], Map.class);
        Map<?,?> toolsResult = (Map<?,?>) tools.get("result");
        List<?> toolList = (List<?>) toolsResult.get("tools");
        assertTrue(toolList.stream().anyMatch(t -> ((Map<?,?>) t).get("name").equals("cn1_lint_code")));

        Map<?,?> lint = mapper.readValue(lines[2], Map.class);
        Map<?,?> lintResult = (Map<?,?>) lint.get("result");
        List<?> content = (List<?>) lintResult.get("content");
        Map<?,?> textPart = (Map<?,?>) content.get(0);
        String payload = (String) textPart.get("text");
        Map<?,?> lintPayload = mapper.readValue(payload, Map.class);
        assertEquals(Boolean.TRUE, lintPayload.get("ok"));
    }

    @Test
    void guideModeListsMarkdownResources() throws Exception {
        Path cacheDir = Files.createTempDirectory("cn1-mcp-test-cache");
        String requests = String.join("\n", List.of(
                "{\"jsonrpc\":\"2.0\",\"id\":10,\"method\":\"initialize\",\"params\":{}}",
                "{\"jsonrpc\":\"2.0\",\"id\":11,\"method\":\"modes/list\",\"params\":{}}",
                "{\"jsonrpc\":\"2.0\",\"id\":12,\"method\":\"modes/set\",\"params\":{\"mode\":\"cn1_guide\"}}",
                "{\"jsonrpc\":\"2.0\",\"id\":13,\"method\":\"resources/list\",\"params\":{}}",
                "{\"jsonrpc\":\"2.0\",\"id\":14,\"method\":\"resources/read\",\"params\":{\"uri\":\"guide://cn1-idioms\"}}"
        )) + "\n";

        var in = new ByteArrayInputStream(requests.getBytes(StandardCharsets.UTF_8));
        var out = new ByteArrayOutputStream();

        StdIoMcpMain.runWithStreams(in, out, new String[]{"--cn1.cacheDir=" + cacheDir.toAbsolutePath()});

        String[] lines = out.toString(StandardCharsets.UTF_8).trim().split("\\R");
        assertEquals(5, lines.length, "Expected five JSON-RPC responses");

        ObjectMapper mapper = new ObjectMapper();

        Map<?,?> modes = mapper.readValue(lines[1], Map.class);
        Map<?,?> modesResult = (Map<?,?>) modes.get("result");
        List<?> modeList = (List<?>) modesResult.get("modes");
        assertTrue(modeList.stream().anyMatch(m -> "cn1_guide".equals(((Map<?,?>) m).get("name"))));

        Map<?,?> setRes = mapper.readValue(lines[2], Map.class);
        Map<?,?> setResult = (Map<?,?>) setRes.get("result");
        assertEquals("cn1_guide", setResult.get("mode"));

        Map<?,?> resourcesList = mapper.readValue(lines[3], Map.class);
        Map<?,?> resourcesResult = (Map<?,?>) resourcesList.get("result");
        List<?> resources = (List<?>) resourcesResult.get("resources");
        assertFalse(resources.isEmpty(), "Guide mode should expose markdown resources");
        @SuppressWarnings("unchecked")
        Map<String,Object> firstResource = (Map<String,Object>) resources.get(0);
        assertEquals("guide://cn1-idioms", firstResource.get("uri"));
        assertEquals("CN1 Idioms", firstResource.get("name"));
        assertEquals("CN1 Idioms (Codename One guide)", firstResource.get("description"));
        assertEquals("text/markdown", firstResource.get("mimeType"));

        Map<?,?> read = mapper.readValue(lines[4], Map.class);
        Map<?,?> readResult = (Map<?,?>) read.get("result");
        List<?> contents = (List<?>) readResult.get("contents");
        Map<?,?> content = (Map<?,?>) contents.get(0);
        String text = (String) content.get("text");
        assertEquals("guide://cn1-idioms", content.get("uri"));
        assertEquals("text/markdown", content.get("mimeType"));
        assertTrue(text.contains("Codename One Idioms"));
    }
}
