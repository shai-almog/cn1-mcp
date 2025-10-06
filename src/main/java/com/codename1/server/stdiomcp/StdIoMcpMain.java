package com.codename1.server.stdiomcp;

import com.codename1.server.mcp.McpApplication;
import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.LintRequest;
import com.codename1.server.mcp.service.ExternalCompileService;
import com.codename1.server.mcp.service.LintService;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

public class StdIoMcpMain {

    record RpcReq(String jsonrpc, Object id, String method, Map<String,Object> params) {}
    record RpcRes(String jsonrpc, Object id, Object result) {}
    record RpcErr(String jsonrpc, Object id, Map<String,Object> error) {}

    public static void main(String[] args) throws Exception {
        // keep stdout clean; route logs to stderr or disable via profile
        System.setProperty("org.springframework.boot.logging.LoggingSystem","none");

        ConfigurableApplicationContext ctx =
                new SpringApplicationBuilder(McpApplication.class)
                        .web(WebApplicationType.NONE)
                        .logStartupInfo(false)
                        .run(args);

        var lint = ctx.getBean(LintService.class);
        var compile = ctx.getBean(ExternalCompileService.class);

        var mapper = new ObjectMapper();
        var in  = new BufferedReader(new InputStreamReader(System.in));
        var out = new BufferedWriter(new OutputStreamWriter(System.out));

        while (true) {
            String line = in.readLine();
            if (line == null) break;
            if (line.isBlank()) continue;

            RpcReq req;
            try {
                req = mapper.readValue(line, RpcReq.class);
            } catch (Exception e) {
                // malformed input -> JSON-RPC error with null id
                writeJson(out, mapper, new RpcErr("2.0", null, Map.of(
                        "code", -32700, "message", "Parse error")));
                continue;
            }

            try {
                switch (req.method) {
                    case "initialize" -> {
                        var result = Map.of(
                                "protocolVersion", req.params.getOrDefault("protocolVersion", "2025-06-18"),
                                "serverInfo", Map.of("name","cn1-mcp","version","0.1.0"),
                                "capabilities", Map.of(
                                        "tools", Map.of(),
                                        "prompts", Map.of(),
                                        "resources", Map.of()
                                )
                        );
                        writeJson(out, mapper, new RpcRes("2.0", req.id, result));
                    }

                    case "tools/list" -> {
                        var tools = List.of(
                                Map.of(
                                        "name","cn1_lint_code",
                                        "description","Lint Java for Codename One",
                                        "inputSchema", Map.of(
                                                "type","object",
                                                "properties", Map.of("code", Map.of("type","string")),
                                                "required", List.of("code")
                                        )
                                ),
                                Map.of(
                                        "name","cn1_compile_check",
                                        "description","Compile Java 8 against CN1 jars",
                                        "inputSchema", Map.of(
                                                "type","object",
                                                "properties", Map.of(
                                                        "files", Map.of(
                                                                "type","array",
                                                                "items", Map.of(
                                                                        "type","object",
                                                                        "properties", Map.of("path", Map.of("type","string"), "content", Map.of("type","string")),
                                                                        "required", List.of("path","content")
                                                                )
                                                        )
                                                ),
                                                "required", List.of("files")
                                        )
                                )
                        );
                        writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("tools", tools)));
                    }

                    case "tools/call" -> {
                        String name = (String) req.params.get("name");
                        @SuppressWarnings("unchecked")
                        Map<String,Object> params = (Map<String,Object>) req.params.getOrDefault("arguments", Map.of());

                        Object toolPayload;
                        switch (name) {
                            case "cn1_lint_code" -> {
                                String code = (String) params.get("code");
                                toolPayload = lint.lint(new LintRequest(code, "java", List.of()));
                            }
                            case "cn1_compile_check" -> {
                                @SuppressWarnings("unchecked")
                                var files = ((List<Map<String,Object>>) params.get("files")).stream()
                                        .map(m -> new FileEntry((String)m.get("path"), (String)m.get("content"))).toList();
                                toolPayload = compile.compile(new CompileRequest(files, null));
                            }
                            default -> throw new IllegalArgumentException("Unknown tool: " + name);
                        }

                        // Wrap as content array (text block). If you prefer structured, you can also return the object directly—
                        // but this format is universally accepted by Claude frontends.
                        var result = Map.of("content", List.of(
                                Map.of("type", "text", "text", mapper.writeValueAsString(toolPayload))
                        ));
                        writeJson(out, mapper, new RpcRes("2.0", req.id, result));
                    }

                    case "notifications/initialized", "ping" -> {
                        // no response required for notifications; for ping, you can echo a result if the client sends a request id
                        if (req.id != null) writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("ok", true)));
                    }

                    case "prompts/list" -> {
                        writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("prompts", List.of())));
                    }
                    case "resources/list" -> {
                        writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("resources", List.of())));
                    }

                    default -> {
                        writeJson(out, mapper, new RpcErr("2.0", req.id, Map.of(
                                "code", -32601, "message", "Method not found: " + req.method)));
                    }
                }
            } catch (Exception ex) {
                writeJson(out, mapper, new RpcErr("2.0", req.id, Map.of(
                        "code", -32000, "message", ex.toString())));
            }
        }
    }

    private static void writeJson(BufferedWriter out, ObjectMapper mapper, Object obj) throws IOException {
        out.write(mapper.writeValueAsString(obj));
        out.write("\n");
        out.flush();
    }
}