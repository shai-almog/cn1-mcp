package com.codename1.server.mcp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codename1.server.mcp.dto.AutoFixRequest;
import com.codename1.server.mcp.dto.AutoFixResponse;
import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.CompileResponse;
import com.codename1.server.mcp.dto.CssCompileRequest;
import com.codename1.server.mcp.dto.CssCompileResponse;
import com.codename1.server.mcp.dto.ExplainRequest;
import com.codename1.server.mcp.dto.ExplainResponse;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.LintRequest;
import com.codename1.server.mcp.dto.LintResponse;
import com.codename1.server.mcp.dto.NativeStubRequest;
import com.codename1.server.mcp.dto.NativeStubResponse;
import com.codename1.server.mcp.dto.Patch;
import com.codename1.server.mcp.dto.ScaffoldRequest;
import com.codename1.server.mcp.dto.ScaffoldResponse;
import com.codename1.server.mcp.dto.Snippet;
import com.codename1.server.mcp.dto.SnippetsRequest;
import com.codename1.server.mcp.dto.SnippetsResponse;
import com.codename1.server.mcp.service.CssCompileService;
import com.codename1.server.mcp.service.ExternalCompileService;
import com.codename1.server.mcp.service.LintService;
import com.codename1.server.mcp.service.NativeStubService;
import com.codename1.server.mcp.service.ScaffoldService;
import com.codename1.server.mcp.service.SnippetService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToolsControllerTest {

  private LintService lintService;
  private ExternalCompileService compileService;
  private CssCompileService cssCompileService;
  private ScaffoldService scaffoldService;
  private SnippetService snippetService;
  private NativeStubService nativeStubService;

  private ToolsController controller;

  @BeforeEach
  void setUp() {
    lintService = mock(LintService.class);
    compileService = mock(ExternalCompileService.class);
    cssCompileService = mock(CssCompileService.class);
    scaffoldService = mock(ScaffoldService.class);
    snippetService = mock(SnippetService.class);
    nativeStubService = mock(NativeStubService.class);

    controller =
        new ToolsController(
            lintService,
            compileService,
            cssCompileService,
            scaffoldService,
            snippetService,
            nativeStubService);
  }

  @Test
  void lintDelegatesToService() {
    LintRequest request = new LintRequest("code", "java", List.of());
    LintResponse response = new LintResponse(true, List.of(), List.of());
    doReturn(response).when(lintService).lint(request);

    assertThat(controller.lint(request)).isSameAs(response);
    verify(lintService).lint(request);
  }

  @Test
  void compileDelegatesToService() {
    CompileRequest request = new CompileRequest(List.of(new FileEntry("A.java", "")), null);
    CompileResponse response = new CompileResponse(true, "ok", List.of());
    doReturn(response).when(compileService).compile(request);

    assertThat(controller.compile(request)).isSameAs(response);
    verify(compileService).compile(request);
  }

  @Test
  void cssCompileDelegatesToService() {
    CssCompileRequest request = new CssCompileRequest(List.of(), "theme.css", "theme.res");
    CssCompileResponse response = new CssCompileResponse(true, "done");
    doReturn(response).when(cssCompileService).compile(request);

    assertThat(controller.compileCss(request)).isSameAs(response);
    verify(cssCompileService).compile(request);
  }

  @Test
  void scaffoldDelegatesToService() {
    ScaffoldRequest request = new ScaffoldRequest("App", "com.example", List.of());
    ScaffoldResponse response = new ScaffoldResponse(List.of());
    doReturn(response).when(scaffoldService).scaffold(request);

    assertThat(controller.scaffold(request)).isSameAs(response);
    verify(scaffoldService).scaffold(request);
  }

  @Test
  void explainDelegatesToSnippetService() {
    ExplainRequest request = new ExplainRequest("rule");
    ExplainResponse response = new ExplainResponse("summary", "before", "after");
    doReturn(response).when(snippetService).explain("rule");

    assertThat(controller.explain(request)).isSameAs(response);
    verify(snippetService).explain("rule");
  }

  @Test
  void searchSnippetsDelegatesToSnippetService() {
    SnippetsRequest request = new SnippetsRequest("topic");
    SnippetsResponse response = new SnippetsResponse(List.of(new Snippet("t", "d", "b")));
    doReturn(response).when(snippetService).get("topic");

    assertThat(controller.searchSnippets(request)).isSameAs(response);
    verify(snippetService).get("topic");
  }

  @Test
  void autoFixWrapsShowCalls() {
    AutoFixRequest request = new AutoFixRequest("form.show();", List.of());

    AutoFixResponse response = controller.autoFix(request);

    assertThat(response.patchedCode())
        .contains("callSerially(() -> { form.show(); });")
        .isNotEqualTo(request.code());
    assertThat(response.patches())
        .singleElement()
        .extracting(Patch::description)
        .isEqualTo("Wrap show() in EDT");
  }

  @Test
  void generateNativeStubsDelegatesToService() {
    NativeStubRequest request =
        new NativeStubRequest(List.of(new FileEntry("MyStub.java", "content")), "MyStub");
    NativeStubResponse response = new NativeStubResponse(List.of());
    doReturn(response).when(nativeStubService).generate(request);

    assertThat(controller.generateNativeStubs(request)).isSameAs(response);
    verify(nativeStubService).generate(request);
  }
}
