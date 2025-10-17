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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  /**
   * Creates the MCP tool registry backed by the Codename One services.
   *
   * @param lintService Codename One lint service
   * @param compileService Codename One Java compile service
   * @param cssCompileService Codename One CSS compile service
   * @param scaffoldService Codename One project scaffolding service
   * @param snippetService Codename One snippet retrieval service
   * @param nativeStubService Codename One native stub generation service
   * @param objectMapper Jackson mapper used to serialise tool payloads
   */
  public CodenameOneMcpTools(
      LintService lintService,
      ExternalCompileService compileService,
      CssCompileService cssCompileService,
      ScaffoldService scaffoldService,
      SnippetService snippetService,
      NativeStubService nativeStubService,
      ObjectMapper objectMapper) {
    this.lintService = Objects.requireNonNull(lintService, "lintService");
    this.compileService = Objects.requireNonNull(compileService, "compileService");
    this.cssCompileService = Objects.requireNonNull(cssCompileService, "cssCompileService");
    this.scaffoldService = Objects.requireNonNull(scaffoldService, "scaffoldService");
    this.snippetService = Objects.requireNonNull(snippetService, "snippetService");
    this.nativeStubService = Objects.requireNonNull(nativeStubService, "nativeStubService");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  /**
   * Runs the Codename One lint tool.
   *
   * @param code Java source code to lint
   * @param language the language identifier, typically {@code java}
   * @param ruleset optional set of lint rule identifiers
   * @return structured lint result payload
   */
  @McpTool(name = "cn1_lint_code", description = "Lint Java for Codename One")
  public McpSchema.CallToolResult lint(
      @McpToolParam(description = "Java source code to lint", required = true) String code,
      @McpToolParam(description = "Language identifier (e.g., java)", required = true)
          String language,
      @McpToolParam(description = "Optional list of lint rules to enforce") List<String> ruleset) {
    LintResponse response = lintService.lint(new LintRequest(code, language, ruleset));
    return structuredResult(response);
  }

  /**
   * Verifies the supplied files compile using the Codename One toolchain.
   *
   * @param files files that make up the compilation unit
   * @param classpathHint optional classpath hint
   * @return structured compile result payload
   */
  @McpTool(name = "cn1_compile_check", description = "Verify code compiles in Codename One")
  public McpSchema.CallToolResult compile(
      @McpToolParam(description = "Files to compile", required = true) List<FileEntry> files,
      @McpToolParam(description = "Optional classpath hint") String classpathHint) {
    CompileResponse response = compileService.compile(new CompileRequest(files, classpathHint));
    return structuredResult(response);
  }

  /**
   * Compiles Codename One CSS themes.
   *
   * @param files CSS files required for compilation
   * @param inputPath primary theme input file
   * @param outputPath optional compiled CSS destination
   * @return structured CSS compile payload
   */
  @McpTool(name = "cn1_compile_css", description = "Compile Codename One CSS themes")
  public McpSchema.CallToolResult compileCss(
      @McpToolParam(description = "CSS files to compile", required = true) List<FileEntry> files,
      @McpToolParam(description = "Theme file to compile", required = true) String inputPath,
      @McpToolParam(description = "Output file name for compiled CSS") String outputPath) {
    CssCompileResponse response =
        cssCompileService.compile(new CssCompileRequest(files, inputPath, outputPath));
    return structuredResult(response);
  }

  /**
   * Scaffolds a new Codename One project structure.
   *
   * @param name display name for the generated project
   * @param pkg base package for generated Java sources
   * @param features optional list of feature identifiers
   * @return structured scaffold payload
   */
  @McpTool(name = "cn1_scaffold_project", description = "Scaffold a new Codename One project")
  public McpSchema.CallToolResult scaffold(
      @McpToolParam(description = "Display name for the project", required = true) String name,
      @McpToolParam(description = "Base Java package for generated sources", required = true)
          String pkg,
      @McpToolParam(description = "Optional feature identifiers to include")
          List<String> features) {
    ScaffoldResponse response =
        scaffoldService.scaffold(new ScaffoldRequest(name, pkg, features));
    return structuredResult(response);
  }

  /**
   * Explains a Codename One lint rule.
   *
   * @param ruleId Codename One lint rule identifier
   * @return structured explanation payload
   */
  @McpTool(name = "cn1_explain_violation", description = "Explain a Codename One lint rule")
  public McpSchema.CallToolResult explain(
      @McpToolParam(description = "Codename One lint rule identifier", required = true)
          String ruleId) {
    ExplainResponse response = snippetService.explain(ruleId);
    return structuredResult(response);
  }

  /**
   * Searches Codename One tutorial snippets for the given topic.
   *
   * @param topic topic to search for
   * @return structured snippet search payload
   */
  @McpTool(name = "cn1_search_snippets", description = "Search Codename One tutorial snippets")
  public McpSchema.CallToolResult searchSnippets(
      @McpToolParam(description = "Topic keyword", required = true) String topic) {
    SnippetsResponse response = snippetService.get(topic);
    return structuredResult(response);
  }

  /**
   * Generates an automatic fix for common Codename One issues.
   *
   * @param code source code to patch
   * @param diagnostics diagnostics describing issues to fix
   * @return structured auto-fix payload
   */
  @McpTool(name = "cn1_auto_fix", description = "Auto-fix common Codename One issues")
  public McpSchema.CallToolResult autoFix(
      @McpToolParam(description = "Source code to patch", required = true) String code,
      @McpToolParam(description = "Diagnostics describing issues to fix")
          List<LintDiag> diagnostics) {
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

  /**
   * Generates native interface stubs for Codename One.
   *
   * @param files compilation unit source files
   * @param interfaceName fully qualified native interface name
   * @return structured native stub payload
   */
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
      Map<String, Object> structured =
          objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
      if (structured != null) {
        builder.structuredContent(Map.copyOf(structured));
      }
    }
    return builder.build();
  }
}
