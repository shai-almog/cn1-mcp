package com.codename1.server.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ProtocolVersions;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Integration tests that exercise the MCP server using the official Java SDK. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      // Ensure the HTTP transport is active even if callers export SPRING_PROFILES_ACTIVE=stdio.
      "spring.main.web-application-type=servlet",
      "spring.ai.mcp.server.stdio=false"
    })
class McpServerIntegrationTest {

  @LocalServerPort private int localPort;

  @Test
  void handshakeNegotiatesProtocolAndCapabilities() {
    try (McpSyncClient client = openClient()) {
      McpSchema.InitializeResult initializeResult = client.initialize();

      assertThat(initializeResult.protocolVersion()).isEqualTo(ProtocolVersions.MCP_2024_11_05);
      assertThat(initializeResult.instructions()).isNull();
      assertThat(initializeResult.meta()).isNull();
      assertThat(initializeResult.serverInfo()).isNotNull();
      assertThat(initializeResult.serverInfo().name()).isEqualTo("codenameone-mcp");
      assertThat(initializeResult.serverInfo().version()).isEqualTo("0.1.0");
      assertThat(initializeResult.capabilities()).isNotNull();
      assertThat(initializeResult.capabilities().tools()).isNotNull();
      assertThat(initializeResult.capabilities().tools().listChanged()).isTrue();
    }
  }

  @Test
  void toolMetadataMatchesRegisteredDefinitions() {
    try (McpSyncClient client = openClient()) {
      client.initialize();

      Map<String, McpSchema.Tool> toolsByName = listToolsByName(client);

      assertThat(toolsByName.keySet())
          .containsExactlyInAnyOrder(
              "cn1_lint_code",
              "cn1_compile_check",
              "cn1_compile_css",
              "cn1_scaffold_project",
              "cn1_explain_violation",
              "cn1_search_snippets",
              "cn1_auto_fix",
              "cn1_generate_native_stubs");

      McpSchema.Tool lintTool = toolsByName.get("cn1_lint_code");
      assertThat(lintTool.description()).isEqualTo("Lint Java for Codename One");
      assertThat(lintTool.inputSchema().type()).isEqualTo("object");
      assertThat(lintTool.inputSchema().required()).contains("code", "language");
      assertThat(lintTool.inputSchema().properties().keySet()).contains("ruleset");

      McpSchema.Tool compileTool = toolsByName.get("cn1_compile_check");
      assertThat(compileTool.description()).isEqualTo("Verify code compiles in Codename One");
      assertThat(compileTool.inputSchema().required()).contains("files");

      McpSchema.Tool stubTool = toolsByName.get("cn1_generate_native_stubs");
      assertThat(stubTool.description())
          .isEqualTo("Generate native interface stubs for Codename One");
      assertThat(stubTool.inputSchema().required()).contains("files", "interfaceName");
    }
  }

  @Test
  void lintToolInvocationReturnsStructuredPayload() {
    try (McpSyncClient client = openClient()) {
      client.initialize();

      McpSchema.CallToolRequest request =
          McpSchema.CallToolRequest.builder()
              .name("cn1_lint_code")
              .arguments(
                  Map.of(
                      "code", "public class Demo { void run() { } }",
                      "language",
                      "java"))
              .build();

      McpSchema.CallToolResult result = client.callTool(request);
      assertThat(result.isError()).isFalse();

      Map<String, Object> payload = readStructuredPayload(result);
      assertThat(payload.get("ok")).isEqualTo(Boolean.TRUE);
      assertThat(payload.get("diagnostics")).isInstanceOf(List.class);
      assertThat(payload.get("quickFixes")).isInstanceOf(List.class);
    }
  }

  @Test
  void callUnknownToolPropagatesProtocolError() {
    try (McpSyncClient client = openClient()) {
      client.initialize();

      McpSchema.CallToolRequest request =
          McpSchema.CallToolRequest.builder().name("does_not_exist").arguments(Map.of()).build();

      assertThatThrownBy(() -> client.callTool(request))
          .isInstanceOf(io.modelcontextprotocol.spec.McpError.class)
          .hasMessageContaining("Unknown tool")
          .satisfies(
              throwable ->
                  assertThat(
                          String.valueOf(
                              ((io.modelcontextprotocol.spec.McpError) throwable)
                                  .getJsonRpcError()
                                  .data()))
                      .contains("does_not_exist"));
    }
  }

  private HttpClientSseClientTransport createTransport() {
    return HttpClientSseClientTransport.builder("http://localhost:" + localPort)
        .sseEndpoint("/sse")
        .build();
  }

  private McpSyncClient openClient() {
    return McpClient.sync(createTransport()).build();
  }

  private Map<String, McpSchema.Tool> listToolsByName(McpSyncClient client) {
    return client.listTools().tools().stream()
        .collect(Collectors.toUnmodifiableMap(McpSchema.Tool::name, Function.identity()));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readStructuredPayload(McpSchema.CallToolResult result) {
    assertThat(result.structuredContent()).isInstanceOf(Map.class);
    return (Map<String, Object>) result.structuredContent();
  }
}
