package com.codename1.server.stdiomcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.CompileResponse;
import com.codename1.server.mcp.dto.CssCompileRequest;
import com.codename1.server.mcp.dto.CssCompileResponse;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.LintRequest;
import com.codename1.server.mcp.dto.LintResponse;
import com.codename1.server.mcp.dto.NativeStubRequest;
import com.codename1.server.mcp.dto.NativeStubResponse;
import com.codename1.server.mcp.service.CssCompileService;
import com.codename1.server.mcp.service.ExternalCompileService;
import com.codename1.server.mcp.service.GuideService;
import com.codename1.server.mcp.service.LintService;
import com.codename1.server.mcp.service.NativeStubService;
import com.codename1.server.mcp.service.GuideService.GuideDoc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ByteArrayResource;

class StdIoMcpMainTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void runWithStreamsProcessesRpcLifecycle() throws Exception {
    LintService lintService = mock(LintService.class);
    ExternalCompileService compileService = mock(ExternalCompileService.class);
    CssCompileService cssService = mock(CssCompileService.class);
    NativeStubService nativeStubService = mock(NativeStubService.class);
    GuideService guideService = mock(GuideService.class);

    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
    doReturn(environment).when(context).getEnvironment();
    doReturn(new String[] {"test"}).when(environment).getActiveProfiles();
    doReturn(lintService).when(context).getBean(LintService.class);
    doReturn(compileService).when(context).getBean(ExternalCompileService.class);
    doReturn(cssService).when(context).getBean(CssCompileService.class);
    doReturn(guideService).when(context).getBean(GuideService.class);
    doReturn(nativeStubService).when(context).getBean(NativeStubService.class);

    GuideDoc guideDoc = new GuideDoc("intro", "Intro", "Desc", new ByteArrayResource("doc".getBytes()));
    doReturn(List.of(guideDoc)).when(guideService).listGuides();
    doReturn(Optional.of(guideDoc)).when(guideService).findGuide("intro");
    doReturn(Optional.empty()).when(guideService).findGuide("missing");
    doReturn("Guide contents").when(guideService).loadGuide("intro");

    doReturn(new LintResponse(true, List.of(), List.of())).when(lintService).lint(any(LintRequest.class));
    doReturn(new CompileResponse(true, "ok", List.of()))
        .when(compileService)
        .compile(any(CompileRequest.class));
    doReturn(new CssCompileResponse(true, "css"))
        .when(cssService)
        .compile(any(CssCompileRequest.class));
    doReturn(new NativeStubResponse(List.of(new FileEntry("Native.java", "body"))))
        .when(nativeStubService)
        .generate(any(NativeStubRequest.class));

    try (MockedConstruction<SpringApplicationBuilder> construction =
        mockConstruction(
            SpringApplicationBuilder.class,
            (builder, contextMock) -> {
              doReturn(builder).when(builder).profiles("stdio");
              doReturn(builder).when(builder).web(WebApplicationType.NONE);
              doReturn(builder).when(builder).logStartupInfo(false);
              doReturn(context).when(builder).run(any(String[].class));
            })) {
      String input = String.join(
              "\n",
              "not-json",
              "",
              "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"1.0\"}}",
              "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}",
              "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"cn1_lint_code\",\"arguments\":{\"code\":\"System.out.println();\"}}}",
              "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"cn1_compile_check\",\"arguments\":{\"files\":[{\"path\":\"Main.java\",\"content\":\"class Main {}\"}]}}}",
              "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"cn1_compile_css\",\"arguments\":{\"files\":[{\"path\":\"theme.css\",\"content\":\"Form{}\"}],\"inputPath\":\"theme.css\",\"outputPath\":\"theme.res\"}}}",
              "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\",\"params\":{\"name\":\"cn1_generate_native_stubs\",\"arguments\":{\"interfaceName\":\"Native\",\"files\":[{\"path\":\"Native.java\",\"content\":\"class Native{}\"}]}}}",
              "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}",
              "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"ping\"}",
              "{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"prompts/list\"}",
              "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"resources/list\"}",
              "{\"jsonrpc\":\"2.0\",\"id\":10,\"method\":\"modes/list\"}",
              "{\"jsonrpc\":\"2.0\",\"id\":11,\"method\":\"modes/set\",\"params\":{\"mode\":\"cn1_guide\"}}",
              "{\"jsonrpc\":\"2.0\",\"id\":12,\"method\":\"resources/list\"}",
              "{\"jsonrpc\":\"2.0\",\"id\":13,\"method\":\"resources/read\",\"params\":{\"uri\":\"guide://intro\"}}",
              "{\"jsonrpc\":\"2.0\",\"id\":14,\"method\":\"resources/read\",\"params\":{\"uri\":\"guide://missing\"}}",
              "{\"jsonrpc\":\"2.0\",\"id\":15,\"method\":\"modes/set\",\"params\":{\"mode\":\"invalid\"}}",
              "{\"jsonrpc\":\"2.0\",\"id\":16,\"method\":\"tools/call\",\"params\":{}}",
              "{\"jsonrpc\":\"2.0\",\"id\":17,\"method\":\"unknown\"}")
          + "\n";

      InputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      StdIoMcpMain.runWithStreams(in, out, new String[] {});

      verify(context).close();
      verify(guideService).listGuides();
      verify(guideService).findGuide("intro");
      verify(guideService).loadGuide("intro");

      String[] lines = out.toString(StandardCharsets.UTF_8).split("\n");
      assertThat(lines.length).isGreaterThanOrEqualTo(17);

      JsonNode first = MAPPER.readTree(lines[0]);
      assertThat(first.get("error").get("code").asInt()).isEqualTo(-32700);

      JsonNode initialize = findResponse(lines, 1);
      assertThat(initialize.get("result").get("serverInfo").get("name").asText())
          .isEqualTo("cn1-mcp");

      JsonNode resourcesRead = findResponse(lines, 13);
      assertThat(resourcesRead.get("result").get("contents").get(0).get("uri").asText())
          .isEqualTo("guide://intro");

      JsonNode error = findResponse(lines, 16);
      assertThat(error.get("error").get("code").asInt()).isEqualTo(-32000);
    }
  }

  private static JsonNode findResponse(String[] lines, int id) throws Exception {
    for (String line : lines) {
      if (line.isBlank()) {
        continue;
      }
      JsonNode node = MAPPER.readTree(line);
      JsonNode nodeId = node.get("id");
      if (nodeId != null && nodeId.asInt() == id) {
        return node;
      }
    }
    throw new AssertionError("No response for id=" + id);
  }
}
