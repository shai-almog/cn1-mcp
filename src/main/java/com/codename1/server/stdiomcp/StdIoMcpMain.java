package com.codename1.server.stdiomcp;

import com.codename1.server.mcp.McpApplication;
import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.LintRequest;
import com.codename1.server.mcp.service.ExternalCompileService;
import com.codename1.server.mcp.service.GuideService;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StdIoMcpMain {

    private static final Logger LOG = LoggerFactory.getLogger(StdIoMcpMain.class);

    record RpcReq(String jsonrpc, Object id, String method, Map<String,Object> params) {}
    record RpcRes(String jsonrpc, Object id, Object result) {}
    record RpcErr(String jsonrpc, Object id, Map<String,Object> error) {}

    public static void main(String[] args) throws Exception {
        runWithStreams(System.in, System.out, args);
    }

    static void runWithStreams(InputStream inStream, OutputStream outStream, String[] args) throws Exception {
        try (ConfigurableApplicationContext ctx =
                     new SpringApplicationBuilder(McpApplication.class)
                             .profiles("stdio")
                             .web(WebApplicationType.NONE)
                             .logStartupInfo(false)
                             .run(args)) {

            var lint = ctx.getBean(LintService.class);
            var compile = ctx.getBean(ExternalCompileService.class);
            var guides = ctx.getBean(GuideService.class);

            final String defaultMode = "default";
            final String guideMode = "cn1_guide";
            final String[] activeMode = { defaultMode };

            var mapper = new ObjectMapper();
            try (var in = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));
                 var out = new BufferedWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8))) {

                LOG.info("STDIO MCP started; active profiles: {}", String.join(",", ctx.getEnvironment().getActiveProfiles()));

                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    if (line.isBlank()) continue;

                    RpcReq req;
                    try {
                        req = mapper.readValue(line, RpcReq.class);
                        LOG.debug("Received request id={} method={} params={}", req.id, req.method, req.params);
                    } catch (Exception e) {
                        LOG.warn("Failed to parse incoming payload: {}", line, e);
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
                                                "resources", Map.of(),
                                                "modes", Map.of()
                                        )
                                );
                                LOG.info("Handled initialize request id={}", req.id);
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
                                LOG.info("Listed tools for request id={} ({} tools)", req.id, tools.size());
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
                                        LOG.info("Invoking lint tool for request id={} ({} chars)", req.id, code != null ? code.length() : 0);
                                        toolPayload = lint.lint(new LintRequest(code, "java", List.of()));
                                    }
                                    case "cn1_compile_check" -> {
                                        @SuppressWarnings("unchecked")
                                        var files = ((List<Map<String,Object>>) params.get("files")).stream()
                                                .map(m -> new FileEntry((String)m.get("path"), (String)m.get("content"))).toList();
                                        LOG.info("Invoking compile tool for request id={} ({} files)", req.id, files.size());
                                        toolPayload = compile.compile(new CompileRequest(files, null));
                                    }
                                    default -> throw new IllegalArgumentException("Unknown tool: " + name);
                                }

                                // Wrap as content array (text block). If you prefer structured, you can also return the object directly
                                // but this format is universally accepted by Claude frontends.
                                var result = Map.of("content", List.of(
                                        Map.of("type", "text", "text", mapper.writeValueAsString(toolPayload))
                                ));
                                LOG.debug("Tool {} completed for request id={} -> {}", name, req.id, toolPayload);
                                writeJson(out, mapper, new RpcRes("2.0", req.id, result));
                            }

                            case "notifications/initialized", "ping" -> {
                                LOG.debug("Received {} notification for id={}", req.method, req.id);
                                if (req.id != null) {
                                    writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("ok", true)));
                                }
                            }

                            case "prompts/list" -> {
                                LOG.info("Listing prompts for request id={}", req.id);
                                writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("prompts", List.of())));
                            }
                            case "resources/list" -> {
                                LOG.info("Listing resources for request id={} mode={} ", req.id, activeMode[0]);
                                List<Map<String,Object>> resources;
                                if (guideMode.equals(activeMode[0])) {
                                    resources = guides.listGuides().stream().map(guide -> {
                                        Map<String,Object> descriptor = new LinkedHashMap<>();
                                        descriptor.put("uri", "guide://" + guide.id());
                                        descriptor.put("name", guide.title());
                                        descriptor.put("description", guide.description());
                                        descriptor.put("mimeType", "text/markdown");
                                        return descriptor;
                                    }).collect(java.util.stream.Collectors.toList());
                                } else {
                                    resources = List.of();
                                }
                                writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("resources", resources)));
                            }
                            case "resources/read" -> {
                                Map<String,Object> params = req.params != null ? req.params : Map.of();
                                String uri = (String) params.get("uri");
                                if (uri == null) {
                                    throw new IllegalArgumentException("Missing uri parameter for resources/read");
                                }
                                if (!guideMode.equals(activeMode[0])) {
                                    throw new IllegalStateException("Guide resources are only available in guide mode");
                                }
                                final String guideScheme = "guide://";
                                if (!uri.startsWith(guideScheme)) {
                                    throw new IllegalArgumentException("Unsupported guide uri: " + uri);
                                }
                                String guideId = uri.substring(guideScheme.length());
                                var guide = guides.findGuide(guideId)
                                        .orElseThrow(() -> new IllegalArgumentException("Unknown guide: " + guideId));
                                try {
                                    String text = guides.loadGuide(guideId);
                                    Map<String,Object> content = new LinkedHashMap<>();
                                    content.put("uri", uri);
                                    content.put("name", guide.title());
                                    content.put("mimeType", "text/markdown");
                                    content.put("text", text);
                                    LOG.info("Read guide {} ({} chars) for request id={}", guideId, text.length(), req.id);
                                    writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of("contents", List.of(content))));
                                } catch (IOException e) {
                                    LOG.error("Failed to load guide {} for request id={}: {}", guideId, req.id, e.getMessage(), e);
                                    writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of(
                                        "code", -32001,
                                        "message", "Failed to load guide: " + e.getMessage()
                                    )));
                                }
                            }
                            case "modes/list" -> {
                                LOG.info("Listing modes for request id={}", req.id);
                                List<Map<String,Object>> modes = new ArrayList<>();
                                Map<String,Object> defaultDescriptor = new LinkedHashMap<>();
                                defaultDescriptor.put("name", defaultMode);
                                defaultDescriptor.put("description", "Default Codename One tooling");
                                defaultDescriptor.put("instructions", "Use lint/compile tools for general development.");
                                defaultDescriptor.put("isDefault", Boolean.TRUE);
                                modes.add(defaultDescriptor);

                                Map<String,Object> guideDescriptor = new LinkedHashMap<>();
                                guideDescriptor.put("name", guideMode);
                                guideDescriptor.put("description", "Browse Codename One onboarding guides");
                                guideDescriptor.put("instructions", "Call resources/list and resources/read to view Markdown guides.");
                                guideDescriptor.put("isDefault", Boolean.FALSE);
                                modes.add(guideDescriptor);
                                Map<String,Object> result = new LinkedHashMap<>();
                                result.put("modes", modes);
                                result.put("current", activeMode[0]);
                                writeJson(out, mapper, new RpcRes("2.0", req.id, result));
                            }
                            case "modes/set" -> {
                                Map<String,Object> params = req.params != null ? req.params : Map.of();
                                String requested = (String) params.get("mode");
                                if (requested == null) {
                                    throw new IllegalArgumentException("Missing mode parameter for modes/set");
                                }
                                if (!requested.equals(defaultMode) && !requested.equals(guideMode)) {
                                    throw new IllegalArgumentException("Unsupported mode: " + requested);
                                }
                                activeMode[0] = requested;
                                LOG.info("Mode set to {} for request id={}", requested, req.id);
                                writeJson(out, mapper, new RpcRes("2.0", req.id, Map.of(
                                        "mode", requested
                                )));
                            }

                            default -> {
                                LOG.warn("Received unknown method {} for request id={}", req.method, req.id);
                                writeJson(out, mapper, new RpcErr("2.0", req.id, Map.of(
                                        "code", -32601, "message", "Method not found: " + req.method)));
                            }
                        }
                    } catch (Exception ex) {
                        LOG.error("Error processing request id={}", req.id, ex);
                        writeJson(out, mapper, new RpcErr("2.0", req.id, Map.of(
                                "code", -32000, "message", ex.toString())));
                    }
                }

                LOG.info("STDIO MCP shutting down");
            }
        }
    }

    private static void writeJson(BufferedWriter out, ObjectMapper mapper, Object obj) throws IOException {
        out.write(mapper.writeValueAsString(obj));
        out.write("\n");
        out.flush();
    }
}
