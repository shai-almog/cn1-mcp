package com.codename1.server.stdiomcp;

import com.codename1.server.mcp.McpApplication;
import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.CssCompileRequest;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.LintRequest;
import com.codename1.server.mcp.dto.NativeStubRequest;
import com.codename1.server.mcp.service.CssCompileService;
import com.codename1.server.mcp.service.ExternalCompileService;
import com.codename1.server.mcp.service.GuideService;
import com.codename1.server.mcp.service.LintService;
import com.codename1.server.mcp.service.NativeStubService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** STDIO entry point that exposes the MCP interface on standard IO streams. */
public final class StdIoMcpMain {

  private static final Logger LOG = LoggerFactory.getLogger(StdIoMcpMain.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String DEFAULT_MODE = "default";
  private static final String GUIDE_MODE = "cn1_guide";
  private static final List<Map<String, Object>> TOOL_DESCRIPTORS = createToolDescriptors();

  private StdIoMcpMain() {}

  record RpcReq(String jsonrpc, Object id, String method, Map<String, Object> params) {}

  record RpcRes(String jsonrpc, Object id, Object result) {}

  record RpcErr(String jsonrpc, Object id, Map<String, Object> error) {}

  public static void main(String[] args) throws Exception {
    runWithStreams(System.in, System.out, args);
  }

  static void runWithStreams(InputStream inStream, OutputStream outStream, String[] args)
      throws Exception {
    try (ConfigurableApplicationContext ctx = createContext(args)) {
      LintService lint = ctx.getBean(LintService.class);
      ExternalCompileService compile = ctx.getBean(ExternalCompileService.class);
      CssCompileService cssCompile = ctx.getBean(CssCompileService.class);
      GuideService guides = ctx.getBean(GuideService.class);
      NativeStubService nativeStubs = ctx.getBean(NativeStubService.class);

      ModeState mode = new ModeState();
      try (BufferedReader in = reader(inStream);
          BufferedWriter out = writer(outStream)) {
        LOG.info(
            "STDIO MCP started; active profiles: {}",
            String.join(",", ctx.getEnvironment().getActiveProfiles()));
        String line;
        while ((line = in.readLine()) != null) {
          if (line.isBlank()) {
            continue;
          }
          RpcReq req;
          try {
            req = MAPPER.readValue(line, RpcReq.class);
            LOG.debug(
                "Received request id={} method={} params={}",
                req.id(),
                req.method(),
                req.params());
          } catch (IOException | RuntimeException e) {
            LOG.warn("Failed to parse incoming payload: {}", line, e);
            writeJson(
                out,
                new RpcErr(
                    "2.0", null, Map.of("code", -32700, "message", "Parse error")));
            continue;
          }

          try {
            handleRequest(req, out, lint, compile, cssCompile, guides, nativeStubs, mode);
          } catch (IOException | RuntimeException ex) {
            LOG.error("Error processing request id={}", req.id(), ex);
            writeJson(
                out,
                new RpcErr("2.0", req.id(), Map.of("code", -32000, "message", ex.toString())));
          }
        }
        LOG.info("STDIO MCP shutting down");
      }
    }
  }

  private static ConfigurableApplicationContext createContext(String[] args) {
    return new SpringApplicationBuilder(McpApplication.class)
        .profiles("stdio")
        .web(WebApplicationType.NONE)
        .logStartupInfo(false)
        .run(args);
  }

  private static BufferedReader reader(InputStream inStream) {
    return new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));
  }

  private static BufferedWriter writer(OutputStream outStream) {
    return new BufferedWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8));
  }

  private static List<Map<String, Object>> createToolDescriptors() {
    List<Map<String, Object>> descriptors = new ArrayList<>();
    descriptors.add(lintToolDescriptor());
    descriptors.add(compileToolDescriptor());
    descriptors.add(cssToolDescriptor());
    descriptors.add(nativeStubToolDescriptor());
    return List.copyOf(descriptors);
  }

  private static Map<String, Object> lintToolDescriptor() {
    Map<String, Object> descriptor = new LinkedHashMap<>();
    descriptor.put("name", "cn1_lint_code");
    descriptor.put("description", "Lint Java for Codename One");
    descriptor.put(
        "inputSchema",
        Map.of(
            "type",
            "object",
            "properties",
            Map.of("code", Map.of("type", "string")),
            "required",
            List.of("code")));
    return Map.copyOf(descriptor);
  }

  private static Map<String, Object> compileToolDescriptor() {
    Map<String, Object> fileSchema =
        Map.of(
            "type",
            "object",
            "properties",
            Map.of(
                "path", Map.of("type", "string"),
                "content", Map.of("type", "string")),
            "required",
            List.of("path", "content"));
    Map<String, Object> descriptor = new LinkedHashMap<>();
    descriptor.put("name", "cn1_compile_check");
    descriptor.put("description", "Compile Java 8 against CN1 jars");
    descriptor.put(
        "inputSchema",
        Map.of(
            "type",
            "object",
            "properties",
            Map.of("files", Map.of("type", "array", "items", fileSchema)),
            "required",
            List.of("files")));
    return Map.copyOf(descriptor);
  }

  private static Map<String, Object> cssToolDescriptor() {
    Map<String, Object> fileSchema =
        Map.of(
            "type",
            "object",
            "properties",
            Map.of(
                "path", Map.of("type", "string"),
                "content", Map.of("type", "string")),
            "required",
            List.of("path", "content"));
    Map<String, Object> descriptor = new LinkedHashMap<>();
    descriptor.put("name", "cn1_compile_css");
    descriptor.put("description", "Compile Codename One CSS using designer.jar");
    descriptor.put(
        "inputSchema",
        Map.of(
            "type",
            "object",
            "properties",
            Map.of(
                "files", Map.of("type", "array", "items", fileSchema),
                "inputPath", Map.of("type", "string"),
                "outputPath", Map.of("type", "string")),
            "required",
            List.of("files")));
    return Map.copyOf(descriptor);
  }

  private static Map<String, Object> nativeStubToolDescriptor() {
    Map<String, Object> fileSchema =
        Map.of(
            "type",
            "object",
            "properties",
            Map.of(
                "path", Map.of("type", "string"),
                "content", Map.of("type", "string")),
            "required",
            List.of("path", "content"));
    Map<String, Object> descriptor = new LinkedHashMap<>();
    descriptor.put("name", "cn1_generate_native_stubs");
    descriptor.put(
        "description", "Generate Codename One native interface stubs");
    descriptor.put(
        "inputSchema",
        Map.of(
            "type",
            "object",
            "properties",
            Map.of(
                "interfaceName", Map.of("type", "string"),
                "files", Map.of("type", "array", "items", fileSchema)),
            "required",
            List.of("interfaceName", "files")));
    return Map.copyOf(descriptor);
  }

  private static void handleRequest(
      RpcReq req,
      BufferedWriter out,
      LintService lint,
      ExternalCompileService compile,
      CssCompileService cssCompile,
      GuideService guides,
      NativeStubService nativeStubs,
      ModeState mode)
      throws IOException {
    Map<String, Object> params = req.params() == null ? Map.of() : req.params();
    switch (req.method()) {
      case "initialize" -> handleInitialize(req, out, params);
      case "tools/list" -> handleToolsList(req, out);
      case "tools/call" ->
          handleToolsCall(req, out, params, lint, compile, cssCompile, nativeStubs);
      case "notifications/initialized", "ping" -> handleNotification(req, out);
      case "prompts/list" -> handlePromptsList(req, out);
      case "resources/list" -> handleResourcesList(req, out, guides, mode);
      case "resources/read" -> handleResourcesRead(req, out, guides, mode, params);
      case "modes/list" -> handleModesList(req, out, mode);
      case "modes/set" -> handleModesSet(req, out, params, mode);
      default ->
          writeJson(
              out,
              new RpcErr(
                  "2.0",
                  req.id(),
                  Map.of("code", -32601, "message", "Method not found: " + req.method())));
    }
  }

  private static void handleInitialize(
      RpcReq req, BufferedWriter out, Map<String, Object> params) throws IOException {
    Map<String, Object> capabilities =
        Map.of(
            "tools", Map.of(),
            "prompts", Map.of(),
            "resources", Map.of(),
            "modes", Map.of());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("protocolVersion", params.getOrDefault("protocolVersion", "2025-06-18"));
    result.put("serverInfo", Map.of("name", "cn1-mcp", "version", "0.1.0"));
    result.put("capabilities", capabilities);
    LOG.info("Handled initialize request id={}", req.id());
    writeJson(out, new RpcRes("2.0", req.id(), result));
  }

  private static void handleToolsList(RpcReq req, BufferedWriter out) throws IOException {
    LOG.info("Listed tools for request id={} ({} tools)", req.id(), TOOL_DESCRIPTORS.size());
    writeJson(out, new RpcRes("2.0", req.id(), Map.of("tools", TOOL_DESCRIPTORS)));
  }

  private static void handleToolsCall(
      RpcReq req,
      BufferedWriter out,
      Map<String, Object> params,
      LintService lint,
      ExternalCompileService compile,
      CssCompileService cssCompile,
      NativeStubService nativeStubs)
      throws IOException {
    Object nameObj = params.get("name");
    if (!(nameObj instanceof String name)) {
      throw new IllegalArgumentException("Missing tool name");
    }
    Map<String, Object> arguments = extractArguments(params);
    Object toolPayload;
    switch (name) {
      case "cn1_lint_code" -> {
        String code = (String) arguments.get("code");
        int length = code != null ? code.length() : 0;
        LOG.info("Invoking lint tool for request id={} ({} chars)", req.id(), length);
        toolPayload = lint.lint(new LintRequest(code, "java", List.of()));
      }
      case "cn1_compile_check" -> {
        List<FileEntry> files = buildFiles(arguments);
        LOG.info("Invoking compile tool for request id={} ({} files)", req.id(), files.size());
        toolPayload = compile.compile(new CompileRequest(files, null));
      }
      case "cn1_compile_css" -> {
        List<FileEntry> files = buildFiles(arguments);
        String inputPath = (String) arguments.get("inputPath");
        String outputPath = (String) arguments.get("outputPath");
        LOG.info(
            "Invoking CSS compile tool for request id={} ({} files) input={} output={}",
            req.id(),
            files.size(),
            inputPath,
            outputPath);
        toolPayload = cssCompile.compile(new CssCompileRequest(files, inputPath, outputPath));
      }
      case "cn1_generate_native_stubs" -> {
        List<FileEntry> files = buildFiles(arguments);
        String interfaceName = (String) arguments.get("interfaceName");
        LOG.info(
            "Invoking native stub generator for request id={} interface={} ({} files)",
            req.id(),
            interfaceName,
            files.size());
        toolPayload = nativeStubs.generate(new NativeStubRequest(files, interfaceName));
      }
      default -> throw new IllegalArgumentException("Unknown tool: " + name);
    }

    Map<String, Object> response =
        Map.of(
            "content",
            List.of(
                Map.of(
                    "type",
                    "text",
                    "text",
                    MAPPER.writeValueAsString(toolPayload))));
    LOG.debug("Tool {} completed for request id={} -> {}", name, req.id(), toolPayload);
    writeJson(out, new RpcRes("2.0", req.id(), response));
  }

  private static Map<String, Object> extractArguments(Map<String, Object> params) {
    Object arguments = params.get("arguments");
    if (arguments instanceof Map<?, ?> map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> cast = (Map<String, Object>) map;
      return cast;
    }
    return Map.of();
  }

  private static List<FileEntry> buildFiles(Map<String, Object> arguments) {
    Object filesObj = arguments.get("files");
    if (!(filesObj instanceof List<?> rawList)) {
      return List.of();
    }
    List<FileEntry> entries = new ArrayList<>();
    for (Object element : rawList) {
      if (element instanceof Map<?, ?> map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> cast = (Map<String, Object>) map;
        String path = (String) cast.get("path");
        String content = (String) cast.get("content");
        if (path != null && content != null) {
          entries.add(new FileEntry(path, content));
        }
      }
    }
    return entries;
  }

  private static void handleNotification(RpcReq req, BufferedWriter out) throws IOException {
    LOG.debug("Received {} notification for id={}", req.method(), req.id());
    if (req.id() != null) {
      writeJson(out, new RpcRes("2.0", req.id(), Map.of("ok", true)));
    }
  }

  private static void handlePromptsList(RpcReq req, BufferedWriter out) throws IOException {
    LOG.info("Listing prompts for request id={}", req.id());
    writeJson(out, new RpcRes("2.0", req.id(), Map.of("prompts", List.of())));
  }

  private static void handleResourcesList(
      RpcReq req, BufferedWriter out, GuideService guides, ModeState mode) throws IOException {
    LOG.info("Listing resources for request id={} mode={}", req.id(), mode.current);
    List<Map<String, Object>> resources;
    if (GUIDE_MODE.equals(mode.current)) {
      resources = guides.listGuides().stream()
          .map(
              guide -> {
                Map<String, Object> descriptor = new LinkedHashMap<>();
                descriptor.put("uri", "guide://" + guide.id());
                descriptor.put("name", guide.title());
                descriptor.put("description", guide.description());
                descriptor.put("mimeType", "text/markdown");
                return descriptor;
              })
          .toList();
    } else {
      resources = List.of();
    }
    writeJson(out, new RpcRes("2.0", req.id(), Map.of("resources", resources)));
  }

  private static void handleResourcesRead(
      RpcReq req,
      BufferedWriter out,
      GuideService guides,
      ModeState mode,
      Map<String, Object> params)
      throws IOException {
    if (!GUIDE_MODE.equals(mode.current)) {
      throw new IllegalStateException("Guide resources are only available in guide mode");
    }
    String uri = (String) params.get("uri");
    if (uri == null) {
      throw new IllegalArgumentException("Missing uri parameter for resources/read");
    }
    final String guideScheme = "guide://";
    if (!uri.startsWith(guideScheme)) {
      throw new IllegalArgumentException("Unsupported guide uri: " + uri);
    }
    String guideId = uri.substring(guideScheme.length());
    var guide =
        guides
            .findGuide(guideId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown guide: " + guideId));
    try {
      String text = guides.loadGuide(guideId);
      Map<String, Object> content = new LinkedHashMap<>();
      content.put("uri", uri);
      content.put("name", guide.title());
      content.put("mimeType", "text/markdown");
      content.put("text", text);
      LOG.info("Read guide {} ({} chars) for request id={}", guideId, text.length(), req.id());
      writeJson(out, new RpcRes("2.0", req.id(), Map.of("contents", List.of(content))));
    } catch (IOException e) {
      LOG.error(
          "Failed to load guide {} for request id={}: {}",
          guideId,
          req.id(),
          e.getMessage(),
          e);
      writeJson(
          out,
          new RpcErr(
              "2.0",
              req.id(),
              Map.of("code", -32001, "message", "Failed to load guide: " + e.getMessage())));
    }
  }

  private static void handleModesList(RpcReq req, BufferedWriter out, ModeState mode)
      throws IOException {
    LOG.info("Listing modes for request id={}", req.id());
    Map<String, Object> defaultDescriptor = new LinkedHashMap<>();
    defaultDescriptor.put("name", DEFAULT_MODE);
    defaultDescriptor.put("description", "Default Codename One tooling");
    defaultDescriptor.put(
        "instructions", "Use lint/compile tools for general development.");
    defaultDescriptor.put("isDefault", Boolean.TRUE);

    Map<String, Object> guideDescriptor = new LinkedHashMap<>();
    guideDescriptor.put("name", GUIDE_MODE);
    guideDescriptor.put("description", "Browse Codename One onboarding guides");
    guideDescriptor.put(
        "instructions", "Call resources/list and resources/read to view Markdown guides.");
    guideDescriptor.put("isDefault", Boolean.FALSE);

    List<Map<String, Object>> modes = new ArrayList<>();
    modes.add(defaultDescriptor);
    modes.add(guideDescriptor);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("modes", modes);
    result.put("current", mode.current);
    writeJson(out, new RpcRes("2.0", req.id(), result));
  }

  private static void handleModesSet(
      RpcReq req, BufferedWriter out, Map<String, Object> params, ModeState mode)
      throws IOException {
    Object requested = params.get("mode");
    if (!(requested instanceof String value)) {
      throw new IllegalArgumentException("Missing mode parameter for modes/set");
    }
    if (!Objects.equals(value, DEFAULT_MODE) && !Objects.equals(value, GUIDE_MODE)) {
      throw new IllegalArgumentException("Unsupported mode: " + value);
    }
    mode.current = value;
    LOG.info("Mode set to {} for request id={}", value, req.id());
    writeJson(out, new RpcRes("2.0", req.id(), Map.of("mode", value)));
  }

  private static void writeJson(BufferedWriter out, Object obj) throws IOException {
    out.write(MAPPER.writeValueAsString(obj));
    out.write('\n');
    out.flush();
  }

  private static final class ModeState {
    private String current = DEFAULT_MODE;
  }
}
