package com.codename1.server.stdiomcp;

import com.codename1.server.mcp.McpApplication;
import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.LintRequest;
import com.codename1.server.mcp.service.ExternalCompileService;
import com.codename1.server.mcp.service.LintService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(StdIoMcpMain.class);

    record RpcReq(String jsonrpc, Object id, String method, Map<String,Object> params) {}
    record RpcRes(String jsonrpc, Object id, Object result) {}
    record RpcErr(String jsonrpc, Object id, Map<String,Object> error) {}

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx =
                new SpringApplicationBuilder(McpApplication.class)
                        .profiles("stdio")
                        .web(WebApplicationType.NONE)
                        .logStartupInfo(false)
                        .run(args);

        LOG.debug("Application context started with {} beans", ctx.getBeanDefinitionCount());
        var lint = ctx.getBean(LintService.class);
        var compile = ctx.getBean(ExternalCompileService.class);

        var mapper = new ObjectMapper();
        var in  = new BufferedReader(new InputStreamReader(System.in));
        var out = new BufferedWriter(new OutputStreamWriter(System.out));

        LOG.info("STDIO MCP ready. Awaiting JSON-RPC requests");
        while (true) {
            String line = in.readLine();
            if (line == null) {
                LOG.info("STDIN closed; shutting down STDIO MCP");
                break;
            }
            if (line.isBlank()) {
                LOG.trace("Skipping blank input line");
                continue;
            }

            LOG.debug("Received raw JSON-RPC payload: {}", line);

            RpcReq req;
            try {
                req = mapper.readValue(line, RpcReq.class);
            } catch (Exception e) {
                LOG.warn("Failed to parse JSON-RPC request: {}", line, e);
                // malformed input -> JSON-RPC error with null id
                writeJson(out, mapper, new RpcErr("2.0", null, Map.of(
                        "code", -32700, "message", "Parse error")));
                continue;
            }

            try {
                LOG.info("Handling method '{}' with id {}", req.method, req.id);
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
                        LOG.debug("initialize response: {}", result);
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
                        LOG.debug("tools/list returning {} tools", tools.size());
                        writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("tools", tools)));
                    }

                    case "tools/call" -> {
                        String name = (String) req.params.get("name");
                        @SuppressWarnings("unchecked")
                        Map<String,Object> params = (Map<String,Object>) req.params.getOrDefault("arguments", Map.of());

                        Object toolPayload;
                        LOG.info("Invoking tool '{}'", name);
                        switch (name) {
                            case "cn1_lint_code" -> {
                                String code = (String) params.get("code");
                                LOG.debug("Lint request received ({} chars)", code != null ? code.length() : 0);
                                toolPayload = lint.lint(new LintRequest(code, "java", List.of()));
                            }
                            case "cn1_compile_check" -> {
                                @SuppressWarnings("unchecked")
                                var files = ((List<Map<String,Object>>) params.get("files")).stream()
                                        .map(m -> new FileEntry((String)m.get("path"), (String)m.get("content"))).toList();
                                LOG.debug("Compile request received with {} file(s)", files.size());
                                toolPayload = compile.compile(new CompileRequest(files, null));
                            }
                            default -> throw new IllegalArgumentException("Unknown tool: " + name);
                        }

                        var result = Map.of("content", List.of(
                                Map.of("type", "text", "text", mapper.writeValueAsString(toolPayload))
                        ));
                        LOG.debug("tools/call '{}' result: {}", name, result);
                        writeJson(out, mapper, new RpcRes("2.0", req.id, result));
                    }

                    case "notifications/initialized", "ping" -> {
                        LOG.debug("Received notification '{}'", req.method);
                        if (req.id != null) writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("ok", true)));
                    }

                    case "prompts/list" -> {
                        LOG.debug("prompts/list requested");
                        writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("prompts", List.of())));
                    }
                    case "resources/list" -> {
                        LOG.debug("resources/list requested");
                        writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("resources", List.of())));
                    }

                    default -> {
                        LOG.warn("Unknown method '{}'", req.method);
                        writeJson(out, mapper, new RpcErr("2.0", req.id, Map.of(
                                "code", -32601, "message", "Method not found: " + req.method)));
                    }
                }
            } catch (Exception ex) {
                LOG.error("Error handling request {} {}", req.id, req.method, ex);
                writeJson(out, mapper, new RpcErr("2.0", req.id, Map.of(
                        "code", -32000, "message", ex.toString())));
            }
        }
        LOG.info("STDIO MCP terminated");
    }

    private static void writeJson(BufferedWriter out, ObjectMapper mapper, Object obj) throws IOException {
        out.write(mapper.writeValueAsString(obj));
        out.write("\n");
        out.flush();
        LOG.trace("Sent JSON-RPC payload: {}", obj);
    }
}
