package com.codename1.server.mcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Handles the SSE endpoint that announces MCP server readiness and capabilities. */
@RestController
@RequestMapping("/mcp")
public class McpSseController {

  private static final Logger LOG = LoggerFactory.getLogger(McpSseController.class);
  private static final Map<String, Object> READY_PARAMS =
      Map.of("tools", List.of(lintToolDescriptor(), compileToolDescriptor()));

  private final ObjectMapper mapper = new ObjectMapper();

  /** Establishes the SSE connection used by MCP clients to receive readiness updates. */
  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter connect() {
    SseEmitter emitter = new SseEmitter(0L);
    try {
      LOG.info("New SSE client connected to /mcp endpoint");
      Map<String, Object> event =
          Map.of("jsonrpc", "2.0", "method", "server/ready", "params", READY_PARAMS);
      emitter.send(
          SseEmitter.event()
              .name("message")
              .data(mapper.writeValueAsString(event)));
    } catch (IOException e) {
      LOG.error("Failed to initialize SSE connection", e);
      emitter.completeWithError(e);
    }
    return emitter;
  }

  private static Map<String, Object> lintToolDescriptor() {
    Map<String, Object> descriptor = new LinkedHashMap<>();
    descriptor.put("name", "cn1_lint_code");
    descriptor.put("description", "Lint Java for Codename One");
    descriptor.put(
        "input_schema",
        Map.of(
            "type",
            "object",
            "properties",
            Map.of("code", Map.of("type", "string"))));
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
                "content", Map.of("type", "string")));
    Map<String, Object> descriptor = new LinkedHashMap<>();
    descriptor.put("name", "cn1_compile_check");
    descriptor.put("description", "Verify code compiles in Codename One");
    descriptor.put(
        "input_schema",
        Map.of(
            "type",
            "object",
            "properties",
            Map.of(
                "files", Map.of("type", "array", "items", fileSchema))));
    return Map.copyOf(descriptor);
  }
}
