package com.codename1.server.stdiomcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class StdIoMcpMainTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private InputStream originalIn;
    private PrintStream originalOut;
    private PipedOutputStream inputFeeder;
    private BufferedWriter stdinWriter;
    private BufferedReader stdoutReader;
    private Thread mainThread;

    @BeforeEach
    void setUp() throws IOException {
        originalIn = System.in;
        originalOut = System.out;

        var stdIn = new PipedInputStream();
        inputFeeder = new PipedOutputStream(stdIn);
        System.setIn(stdIn);
        stdinWriter = new BufferedWriter(new OutputStreamWriter(inputFeeder, StandardCharsets.UTF_8));

        var stdoutPipe = new PipedInputStream();
        var stdoutSink = new PipedOutputStream(stdoutPipe);
        System.setOut(new PrintStream(stdoutSink, true, StandardCharsets.UTF_8));
        stdoutReader = new BufferedReader(new InputStreamReader(stdoutPipe, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (stdinWriter != null) {
                stdinWriter.close();
            }
            if (inputFeeder != null) {
                inputFeeder.close();
            }
        } finally {
            if (mainThread != null) {
                mainThread.join(5000);
            }
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    @Test
    void stdioMainHandlesInitializeAndLintCall() throws Exception {
        mainThread = new Thread(() -> {
            try {
                StdIoMcpMain.main(new String[]{"--spring.profiles.active=stdio"});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "stdio-main-test");
        mainThread.start();

        // wait briefly for Spring context to spin up
        Thread.sleep(500);

        sendRequest(Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "initialize",
                "params", Map.of("protocolVersion", "2025-06-18")
        ));
        Map<String, Object> initResponse = readJsonResponse();
        assertEquals("2.0", initResponse.get("jsonrpc"));
        assertEquals(1, initResponse.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> initResult = (Map<String, Object>) initResponse.get("result");
        assertNotNull(initResult);
        assertEquals("2025-06-18", initResult.get("protocolVersion"));

        sendRequest(Map.of(
                "jsonrpc", "2.0",
                "id", 2,
                "method", "tools/list",
                "params", Map.of()
        ));
        Map<String, Object> toolsResponse = readJsonResponse();
        assertEquals(2, toolsResponse.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> toolsResult = (Map<String, Object>) toolsResponse.get("result");
        assertNotNull(toolsResult);
        assertTrue(toolsResult.containsKey("tools"));

        sendRequest(Map.of(
                "jsonrpc", "2.0",
                "id", 3,
                "method", "tools/call",
                "params", Map.of(
                        "name", "cn1_lint_code",
                        "arguments", Map.of("code", "public class Foo {}"))
        ));
        Map<String, Object> lintResponse = readJsonResponse();
        assertEquals(3, lintResponse.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> lintResult = (Map<String, Object>) lintResponse.get("result");
        assertNotNull(lintResult);
        assertTrue(lintResult.containsKey("content"));
        assertThat(lintResult.get("content").toString()).contains("\"ok\":true");

        // Close STDIN to terminate the loop cleanly
        stdinWriter.close();
        inputFeeder.close();
        mainThread.join(5000);
        assertFalse(mainThread.isAlive(), "STDIO loop should exit after EOF");
    }

    private void sendRequest(Map<String, Object> payload) throws IOException {
        stdinWriter.write(mapper.writeValueAsString(payload));
        stdinWriter.write("\n");
        stdinWriter.flush();
    }

    private Map<String, Object> readJsonResponse() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> future = executor.submit(stdoutReader::readLine);
            String line = future.get(5, TimeUnit.SECONDS);
            assertNotNull(line, "Expected JSON-RPC response but got EOF");
            assertTrue(line.startsWith("{"), "Response must be JSON but was: " + line);
            return mapper.readValue(line, new TypeReference<>() {});
        } finally {
            executor.shutdownNow();
        }
    }
}
