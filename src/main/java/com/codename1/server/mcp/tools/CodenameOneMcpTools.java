package com.codename1.server.mcp.tools;

import com.codename1.server.mcp.dto.AutoFixResponse;
import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.CompileResponse;
import com.codename1.server.mcp.dto.CssCompileRequest;
import com.codename1.server.mcp.dto.CssCompileResponse;
import com.codename1.server.mcp.dto.ExplainResponse;
import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.LintDiag;
import com.codename1.server.mcp.dto.LintRequest;
import com.codename1.server.mcp.dto.LintResponse;
import com.codename1.server.mcp.dto.NativeStubRequest;
import com.codename1.server.mcp.dto.NativeStubResponse;
import com.codename1.server.mcp.dto.Patch;
import com.codename1.server.mcp.dto.ScaffoldRequest;
import com.codename1.server.mcp.dto.ScaffoldResponse;
import com.codename1.server.mcp.dto.SnippetsResponse;
import com.codename1.server.mcp.service.CssCompileService;
import com.codename1.server.mcp.service.ExternalCompileService;
import com.codename1.server.mcp.service.LintService;
import com.codename1.server.mcp.service.NativeStubService;
import com.codename1.server.mcp.service.ScaffoldService;
import com.codename1.server.mcp.service.SnippetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Registers Codename One operations as Model Context Protocol tools using the official SDK.
 */
@Component
public class CodenameOneMcpTools {

  private final LintService lintService;
  private final ExternalCompileService compileService;
  private final CssCompileService cssCompileService;
  private final ScaffoldService scaffoldService;
  private final SnippetService snippetService;
  private final NativeStubService nativeStubService;
  private final ObjectMapper objectMapper;

  public CodenameOneMcpTools(
      LintService lintService,
      ExternalCompileService compileService,
      CssCompileService cssCompileService,
      ScaffoldService scaffoldService,
      SnippetService snippetService,
      NativeStubService nativeStubService,
      ObjectMapper objectMapper) {
    this.lintService = lintService;
    this.compileService = compileService;
    this.cssCompileService = cssCompileService;
    this.scaffoldService = scaffoldService;
    this.snippetService = snippetService;
    this.nativeStubService = nativeStubService;
    this.objectMapper = objectMapper;
  }

  @McpTool(name = "cn1_lint_code", description = "Lint Java for Codename One")
  public McpSchema.CallToolResult lint(
      @McpToolParam(description = "Java source code to lint", required = true) String code,
      @McpToolParam(description = "Language identifier (e.g., java)", required = true)
          String language,
      @McpToolParam(description = "Optional list of lint rules to enforce") List<String> ruleset) {
    LintResponse response = lintService.lint(new LintRequest(code, language, ruleset));
    return structuredResult(response);
  }

  @McpTool(name = "cn1_compile_check", description = "Verify code compiles in Codename One")
  public McpSchema.CallToolResult compile(
      @McpToolParam(description = "Files to compile", required = true) List<FileEntry> files,
      @McpToolParam(description = "Optional classpath hint") String classpathHint) {
    CompileResponse response = compileService.compile(new CompileRequest(files, classpathHint));
    return structuredResult(response);
  }

  @McpTool(name = "cn1_compile_css", description = "Compile Codename One CSS themes")
  public McpSchema.CallToolResult compileCss(
      @McpToolParam(description = "CSS files to compile", required = true) List<FileEntry> files,
      @McpToolParam(description = "Theme file to compile", required = true) String inputPath,
      @McpToolParam(description = "Output file name for compiled CSS") String outputPath) {
    CssCompileResponse response =
        cssCompileService.compile(new CssCompileRequest(files, inputPath, outputPath));
    return structuredResult(response);
  }

  @McpTool(name = "cn1_scaffold_project", description = "Scaffold a new Codename One project")
  public McpSchema.CallToolResult scaffold(
      @McpToolParam(description = "Display name for the project", required = true) String name,
      @McpToolParam(description = "Base Java package for generated sources", required = true)
          String pkg,
      @McpToolParam(description = "Optional feature identifiers to include") List<String> features) {
    ScaffoldResponse response = scaffoldService.scaffold(new ScaffoldRequest(name, pkg, features));
    return structuredResult(response);
  }

  @McpTool(name = "cn1_explain_violation", description = "Explain a Codename One lint rule")
  public McpSchema.CallToolResult explain(
      @McpToolParam(description = "Codename One lint rule identifier", required = true) String ruleId) {
    ExplainResponse response = snippetService.explain(ruleId);
    return structuredResult(response);
  }

  @McpTool(name = "cn1_search_snippets", description = "Search Codename One tutorial snippets")
  public McpSchema.CallToolResult searchSnippets(
      @McpToolParam(description = "Topic keyword", required = true) String topic) {
    SnippetsResponse response = snippetService.get(topic);
    return structuredResult(response);
  }

  @McpTool(name = "cn1_auto_fix", description = "Auto-fix common Codename One issues")
  public McpSchema.CallToolResult autoFix(
      @McpToolParam(description = "Source code to patch", required = true) String code,
      @McpToolParam(description = "Diagnostics describing issues to fix") List<LintDiag> diagnostics) {
    String source = code;
    if (source == null) {
      source = "";
    }
    String patched =
        source.replace(
            "form.show();",
            "com.codename1.ui.Display.getInstance().callSerially(() -> { form.show(); });");
    String diff =
        String.join(
            "\n",
            "  @@",
            "  - form.show();",
            "  + com.codename1.ui.Display.getInstance().callSerially(() -> { form.show(); });",
            "  ");
    Patch patch = new Patch("Wrap show() in EDT", diff);
    AutoFixResponse response = new AutoFixResponse(patched, List.of(patch));
    return structuredResult(response);
  }

  @McpTool(
      name = "cn1_generate_native_stubs",
      description = "Generate native interface stubs for Codename One")
  public McpSchema.CallToolResult generateNativeStubs(
      @McpToolParam(description = "Compilation unit source files", required = true)
          List<FileEntry> files,
      @McpToolParam(description = "Fully qualified native interface name", required = true)
          String interfaceName) {
    NativeStubResponse response =
        nativeStubService.generate(new NativeStubRequest(files, interfaceName));
    return structuredResult(response);
  }

  private McpSchema.CallToolResult structuredResult(Object value) {
    McpSchema.CallToolResult.Builder builder =
        McpSchema.CallToolResult.builder().isError(Boolean.FALSE);
    if (value != null) {
      Map<String, Object> structured = objectMapper.convertValue(value, Map.class);
      builder.structuredContent(structured);
    }
    return builder.build();
  }
}
