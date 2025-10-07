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
}
