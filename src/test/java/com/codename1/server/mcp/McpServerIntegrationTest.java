package com.codename1.server.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerSseProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/** Integration tests that exercise the MCP server using the official Java SDK. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerIntegrationTest {

  @Autowired private McpServerSseProperties sseProperties;

  @Autowired private ApplicationContext applicationContext;

  @Test
  void exposesCodenameOneToolsOverSseTransport() {
    HttpClientSseClientTransport transport = createTransport();

    try (McpSyncClient client = McpClient.sync(transport).build()) {
      McpSchema.InitializeResult initializeResult = client.initialize();
      assertThat(initializeResult).isNotNull();

      List<String> toolNames =
          client.listTools().tools().stream().map(McpSchema.Tool::name).collect(Collectors.toList());
      assertThat(toolNames)
          .contains("cn1_lint_code", "cn1_compile_check", "cn1_generate_native_stubs");

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
      assertThat(result.structuredContent()).isInstanceOf(Map.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> payload = (Map<String, Object>) result.structuredContent();
      assertThat(payload.get("ok")).isEqualTo(Boolean.TRUE);
    }
  }

  private HttpClientSseClientTransport createTransport() {
    int port = getLocalPort();
    String baseUri = "http://localhost:" + port;
    String basePath = sseProperties.getBaseUrl();
    if (basePath != null && !basePath.isBlank()) {
      baseUri += basePath.startsWith("/") ? basePath : "/" + basePath;
    }
    HttpClientSseClientTransport.Builder builder =
        HttpClientSseClientTransport.builder(baseUri);
    String sseEndpoint = sseProperties.getSseEndpoint();
    if (sseEndpoint != null && !sseEndpoint.isBlank()) {
      builder.sseEndpoint(sseEndpoint);
    }
    return builder.build();
  }

  private int getLocalPort() {
    String value = applicationContext.getEnvironment().getProperty("local.server.port");
    if (value == null) {
      throw new IllegalStateException("local.server.port property is not set");
    }
    return Integer.parseInt(value);
  }
}
