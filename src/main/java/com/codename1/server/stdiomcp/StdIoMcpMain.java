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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
  private static final List<String> SUPPORTED_PROTOCOL_VERSIONS =
      List.of("2024-12-17", "2024-09-18");
  private static final String DEFAULT_PROTOCOL_VERSION = SUPPORTED_PROTOCOL_VERSIONS.get(0);
  private static final String SERVER_NAME = "cn1-mcp";
  private static final String SERVER_VERSION = "0.1.0";
  private static final Map<String, Object> SERVER_INFO =
      Map.of(
          "name",
          SERVER_NAME,
          "version",
          SERVER_VERSION,
          "supportedVersions",
          SUPPORTED_PROTOCOL_VERSIONS);
  private static final Map<String, Object> SERVER_CAPABILITIES =
      Map.of(
          "tools", Map.of("list", Map.of(), "call", Map.of()),
          "prompts", Map.of("list", Map.of(), "call", Map.of()),
          "resources", Map.of("list", Map.of(), "read", Map.of()),
          "modes", Map.of("list", Map.of(), "set", Map.of()));
  private static final List<Map<String, Object>> PROMPT_DESCRIPTORS = createPromptDescriptors();

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
          try {
            JsonNode payload = MAPPER.readTree(line);
            if (payload.isArray()) {
              List<Object> responses = new ArrayList<>();
              for (JsonNode element : payload) {
                Object response =
                    processJsonNode(
                        element, lint, compile, cssCompile, guides, nativeStubs, mode);
                if (response != null) {
                  responses.add(response);
                }
              }
              if (!responses.isEmpty()) {
                writeJson(out, responses);
              }
            } else {
              Object response =
                  processJsonNode(
                      payload, lint, compile, cssCompile, guides, nativeStubs, mode);
              if (response != null) {
                writeJson(out, response);
              }
            }
          } catch (JsonProcessingException e) {
            LOG.warn("Failed to parse incoming payload: {}", line, e);
            writeJson(
                out,
                new RpcErr(
                    "2.0", null, Map.of("code", -32700, "message", "Parse error")));
          }
        }
        LOG.info("STDIO MCP shutting down");
      }
    }
  }

  private static Object processJsonNode(
      JsonNode node,
      LintService lint,
      ExternalCompileService compile,
      CssCompileService cssCompile,
      GuideService guides,
      NativeStubService nativeStubs,
      ModeState mode) {
    if (node == null || node.isNull()) {
      return new RpcErr(
          "2.0", null, Map.of("code", -32600, "message", "Invalid Request: empty payload"));
    }
    if (!node.isObject()) {
      return new RpcErr(
          "2.0",
          null,
          Map.of("code", -32600, "message", "Invalid Request: expected JSON object"));
    }
    RpcReq req;
    try {
      req = MAPPER.treeToValue(node, RpcReq.class);
    } catch (JsonProcessingException e) {
      LOG.warn("Failed to convert payload to request: {}", node, e);
      return new RpcErr(
          "2.0", null, Map.of("code", -32700, "message", "Parse error"));
    }
    LOG.debug(
        "Received request id={} method={} params={}", req.id(), req.method(), req.params());
    Object response =
        executeRequest(req, lint, compile, cssCompile, guides, nativeStubs, mode);
    if (response == null) {
      return null;
    }
    if (response instanceof RpcRes res) {
      LOG.debug("Completed request id={} method={}", res.id(), req.method());
    }
    return response;
  }

  private static Object executeRequest(
      RpcReq req,
      LintService lint,
      ExternalCompileService compile,
      CssCompileService cssCompile,
      GuideService guides,
      NativeStubService nativeStubs,
      ModeState mode) {
    try {
      return dispatchRequest(req, lint, compile, cssCompile, guides, nativeStubs, mode);
    } catch (RuntimeException ex) {
      LOG.error("Error processing request id={}", req.id(), ex);
      return new RpcErr(
          "2.0", req.id(), Map.of("code", -32000, "message", ex.toString()));
    }
  }

  private static Object dispatchRequest(
      RpcReq req,
      LintService lint,
      ExternalCompileService compile,
      CssCompileService cssCompile,
      GuideService guides,
      NativeStubService nativeStubs,
      ModeState mode) {
    String method = req.method();
    if (method == null) {
      return new RpcErr(
          "2.0", req.id(), Map.of("code", -32600, "message", "Invalid Request: missing method"));
    }
    Map<String, Object> params = req.params() == null ? Map.of() : req.params();
    return switch (method) {
      case "initialize" -> handleInitialize(req, params);
      case "server/info" -> handleServerInfo(req);
      case "tools/list" -> handleToolsList(req);
      case "tools/call" -> handleToolsCall(req, params, lint, compile, cssCompile, nativeStubs);
      case "notifications/initialized", "initialized" -> handleNotification(req);
      case "notifications/cancelled", "notifications/canceled", "requests/cancel" ->
          handleNotification(req);
      case "ping" -> handlePing(req);
      case "prompts/list" -> handlePromptsList(req);
      case "prompts/call" -> handlePromptsCall(req, params);
      case "resources/list" -> handleResourcesList(req, guides, mode);
      case "resources/read" -> handleResourcesRead(req, guides, mode, params);
      case "modes/list" -> handleModesList(req, mode);
      case "modes/set" -> handleModesSet(req, params, mode);
      default ->
          new RpcErr(
              "2.0",
              req.id(),
              Map.of("code", -32601, "message", "Method not found: " + method));
    };
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

  private static List<Map<String, Object>> createPromptDescriptors() {
    Map<String, Object> descriptor = new LinkedHashMap<>();
    descriptor.put("name", "cn1_explain_error");
    descriptor.put("description", "Explain a Codename One build or lint error message.");
    descriptor.put(
        "arguments",
        Map.of(
            "type",
            "object",
            "properties",
            Map.of(
                "message",
                Map.of(
                    "type",
                    "string",
                    "description",
                    "The compiler, lint, or build error that needs clarification.")),
            "required",
            List.of("message")));
    return List.of(Map.copyOf(descriptor));
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

  private static Map<String, Object> buildServerMetadata() {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("name", SERVER_NAME);
    metadata.put("version", SERVER_VERSION);
    metadata.put("supportedVersions", SUPPORTED_PROTOCOL_VERSIONS);
    metadata.put("serverInfo", SERVER_INFO);
    metadata.put("capabilities", SERVER_CAPABILITIES);
    return metadata;
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

  private static Object handleInitialize(RpcReq req, Map<String, Object> params) {
    Map<String, Object> result = buildServerMetadata();
    String negotiated = negotiateProtocolVersion(params);
    if (negotiated == null) {
      return new RpcErr(
          "2.0",
          req.id(),
          Map.of(
              "code",
              -32602,
              "message",
              "Unsupported protocol version",
              "data",
              Map.of("supported", SUPPORTED_PROTOCOL_VERSIONS)));
    }
    result.put("protocolVersion", negotiated);
    LOG.info("Handled initialize request id={}", req.id());
    return new RpcRes("2.0", req.id(), result);
  }

  private static Object handleServerInfo(RpcReq req) {
    LOG.info("server/info requested for id={}", req.id());
    Map<String, Object> result = buildServerMetadata();
    result.put("protocolVersion", DEFAULT_PROTOCOL_VERSION);
    return new RpcRes("2.0", req.id(), result);
  }

  private static Object handleToolsList(RpcReq req) {
    LOG.info("Listed tools for request id={} ({} tools)", req.id(), TOOL_DESCRIPTORS.size());
    return new RpcRes("2.0", req.id(), Map.of("tools", TOOL_DESCRIPTORS));
  }

  private static Object handleToolsCall(
      RpcReq req,
      Map<String, Object> params,
      LintService lint,
      ExternalCompileService compile,
      CssCompileService cssCompile,
      NativeStubService nativeStubs) {
    Object nameObj = params.get("name");
    if (!(nameObj instanceof String name)) {
      throw new IllegalArgumentException("Missing tool name");
    }
    Map<String, Object> arguments = extractArguments(params);
    Object toolPayload;
    switch (name) {
      case "cn1_lint_code" -> {
        String code = (String) arguments.get("code");
        if (code == null) {
          throw new IllegalArgumentException("Missing required argument 'code'");
        }
        int length = code.length();
        LOG.info("Invoking lint tool for request id={} ({} chars)", req.id(), length);
        toolPayload = lint.lint(new LintRequest(code, "java", List.of()));
      }
      case "cn1_compile_check" -> {
        List<FileEntry> files = buildFiles(arguments);
        if (files.isEmpty()) {
          throw new IllegalArgumentException("Missing required argument 'files'");
        }
        LOG.info("Invoking compile tool for request id={} ({} files)", req.id(), files.size());
        toolPayload = compile.compile(new CompileRequest(files, null));
      }
      case "cn1_compile_css" -> {
        List<FileEntry> files = buildFiles(arguments);
        if (files.isEmpty()) {
          throw new IllegalArgumentException("Missing required argument 'files'");
        }
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
        if (files.isEmpty()) {
          throw new IllegalArgumentException("Missing required argument 'files'");
        }
        String interfaceName = (String) arguments.get("interfaceName");
        if (interfaceName == null || interfaceName.isBlank()) {
          throw new IllegalArgumentException("Missing required argument 'interfaceName'");
        }
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
            List.of(Map.of("type", "application/json", "json", toolPayload)));
    LOG.debug("Tool {} completed for request id={} -> {}", name, req.id(), toolPayload);
    return new RpcRes("2.0", req.id(), response);
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
      throw new IllegalArgumentException("Missing required argument 'files'");
    }
    if (rawList.isEmpty()) {
      throw new IllegalArgumentException("Missing required argument 'files'");
    }
    List<FileEntry> entries = new ArrayList<>();
    for (Object element : rawList) {
      if (!(element instanceof Map<?, ?> map)) {
        throw new IllegalArgumentException("Each file must be an object with path and content");
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> cast = (Map<String, Object>) map;
      String path = (String) cast.get("path");
      String content = (String) cast.get("content");
      if (path == null || content == null) {
        throw new IllegalArgumentException("Each file requires 'path' and 'content' fields");
      }
      entries.add(new FileEntry(path, content));
    }
    if (entries.isEmpty()) {
      throw new IllegalArgumentException("Missing required argument 'files'");
    }
    return entries;
  }

  private static Object handleNotification(RpcReq req) {
    LOG.debug("Received {} notification for id={}", req.method(), req.id());
    if (req.id() != null) {
      return new RpcRes("2.0", req.id(), Map.of());
    }
    return null;
  }

  private static Object handlePing(RpcReq req) {
    LOG.debug("Handled ping for id={}", req.id());
    return new RpcRes("2.0", req.id(), Map.of());
  }

  private static Object handlePromptsList(RpcReq req) {
    LOG.info("Listing prompts for request id={}", req.id());
    return new RpcRes("2.0", req.id(), Map.of("prompts", PROMPT_DESCRIPTORS));
  }

  private static Object handlePromptsCall(RpcReq req, Map<String, Object> params) {
    Object nameObj = params.get("name");
    if (!(nameObj instanceof String name)) {
      throw new IllegalArgumentException("Missing prompt name");
    }
    LOG.info("Prompt call {} for request id={}", name, req.id());
    Map<String, Object> arguments = extractArguments(params);
    String output;
    if ("cn1_explain_error".equals(name)) {
      String message = (String) arguments.get("message");
      if (message == null || message.isBlank()) {
        throw new IllegalArgumentException("Missing required argument 'message'");
      }
      output =
          "The Codename One tooling received the following error:\n\n"
              + message
              + "\n\n"
              + "Try cleaning the project, ensuring all libraries are present, and rerunning lint.";
    } else {
      throw new IllegalArgumentException("Unknown prompt: " + name);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("content", List.of(Map.of("type", "text", "text", output)));
    return new RpcRes("2.0", req.id(), result);
  }

  private static Object handleResourcesList(RpcReq req, GuideService guides, ModeState mode) {
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
    return new RpcRes("2.0", req.id(), Map.of("resources", resources));
  }

  private static Object handleResourcesRead(
      RpcReq req,
      GuideService guides,
      ModeState mode,
      Map<String, Object> params) {
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
      return new RpcRes("2.0", req.id(), Map.of("contents", List.of(content)));
    } catch (IOException e) {
      LOG.error(
          "Failed to load guide {} for request id={}: {}",
          guideId,
          req.id(),
          e.getMessage(),
          e);
      return new RpcErr(
          "2.0",
          req.id(),
          Map.of("code", -32001, "message", "Failed to load guide: " + e.getMessage()));
    }
  }

  private static Object handleModesList(RpcReq req, ModeState mode) {
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
    return new RpcRes("2.0", req.id(), result);
  }

  private static Object handleModesSet(
      RpcReq req, Map<String, Object> params, ModeState mode) {
    Object requested = params.get("mode");
    if (!(requested instanceof String value)) {
      throw new IllegalArgumentException("Missing mode parameter for modes/set");
    }
    if (!Objects.equals(value, DEFAULT_MODE) && !Objects.equals(value, GUIDE_MODE)) {
      throw new IllegalArgumentException("Unsupported mode: " + value);
    }
    mode.current = value;
    LOG.info("Mode set to {} for request id={}", value, req.id());
    return new RpcRes("2.0", req.id(), Map.of("mode", value));
  }

  private static void writeJson(BufferedWriter out, Object obj) throws IOException {
    out.write(MAPPER.writeValueAsString(obj));
    out.write('\n');
    out.flush();
  }

  private static String negotiateProtocolVersion(Map<String, Object> params) {
    Object requested = params.get("protocolVersion");
    if (requested instanceof String version) {
      if (SUPPORTED_PROTOCOL_VERSIONS.contains(version)) {
        return version;
      }
      LOG.warn(
          "Unsupported protocolVersion '{}' requested; falling back to default", version);
      return DEFAULT_PROTOCOL_VERSION;
    }
    Object versions = params.get("protocolVersions");
    if (versions instanceof List<?> list && !list.isEmpty()) {
      for (String supported : SUPPORTED_PROTOCOL_VERSIONS) {
        if (list.contains(supported)) {
          return supported;
        }
      }
      LOG.warn(
          "No compatible protocolVersions {} requested; falling back to default",
          list);
      return DEFAULT_PROTOCOL_VERSION;
    }
    return DEFAULT_PROTOCOL_VERSION;
  }

  private static final class ModeState {
    private String current = DEFAULT_MODE;
  }
}
