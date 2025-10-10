package com.codename1.server.mcp.controller;

import com.codename1.server.mcp.dto.AutoFixRequest;
import com.codename1.server.mcp.dto.AutoFixResponse;
import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.CompileResponse;
import com.codename1.server.mcp.dto.CssCompileRequest;
import com.codename1.server.mcp.dto.CssCompileResponse;
import com.codename1.server.mcp.dto.ExplainRequest;
import com.codename1.server.mcp.dto.ExplainResponse;
import com.codename1.server.mcp.dto.LintRequest;
import com.codename1.server.mcp.dto.LintResponse;
import com.codename1.server.mcp.dto.NativeStubRequest;
import com.codename1.server.mcp.dto.NativeStubResponse;
import com.codename1.server.mcp.dto.Patch;
import com.codename1.server.mcp.dto.ScaffoldRequest;
import com.codename1.server.mcp.dto.ScaffoldResponse;
import com.codename1.server.mcp.dto.SnippetsRequest;
import com.codename1.server.mcp.dto.SnippetsResponse;
import com.codename1.server.mcp.service.CssCompileService;
import com.codename1.server.mcp.service.ExternalCompileService;
import com.codename1.server.mcp.service.LintService;
import com.codename1.server.mcp.service.NativeStubService;
import com.codename1.server.mcp.service.ScaffoldService;
import com.codename1.server.mcp.service.SnippetService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP facade exposing the Codename One tooling set as MCP-style endpoints. */
@RestController
@RequestMapping("/tools")
public class ToolsController {

  private static final Logger LOG = LoggerFactory.getLogger(ToolsController.class);

  private final LintService lintService;
  private final ExternalCompileService compileService;
  private final CssCompileService cssCompileService;
  private final ScaffoldService scaffoldService;
  private final SnippetService snippetService;
  private final NativeStubService nativeStubService;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects singleton services; controller keeps shared references.")
  public ToolsController(
      LintService lintService,
      ExternalCompileService compileService,
      CssCompileService cssCompileService,
      ScaffoldService scaffoldService,
      SnippetService snippetService,
      NativeStubService nativeStubService) {
    this.lintService = lintService;
    this.compileService = compileService;
    this.cssCompileService = cssCompileService;
    this.scaffoldService = scaffoldService;
    this.snippetService = snippetService;
    this.nativeStubService = nativeStubService;
  }

  /** Runs the Codename One linting rules against the provided request payload. */
  @PostMapping(value = "/cn1_lint_code", consumes = MediaType.APPLICATION_JSON_VALUE)
  public LintResponse lint(@RequestBody LintRequest request) {
    int rulesetSize = request.ruleset() == null ? 0 : request.ruleset().size();
    LOG.info(
        "HTTP lint request received: language={} rulesetSize={}",
        request.language(),
        rulesetSize);
    return lintService.lint(request);
  }

  /** Compiles Java sources using the Codename One toolchain. */
  @PostMapping(value = "/cn1_compile_check", consumes = MediaType.APPLICATION_JSON_VALUE)
  public CompileResponse compile(@RequestBody CompileRequest request) {
    int fileCount = request.files() == null ? 0 : request.files().size();
    LOG.info("HTTP compile request received with {} files", fileCount);
    return compileService.compile(request);
  }

  /** Compiles Codename One CSS into a theme resource. */
  @PostMapping(value = "/cn1_compile_css", consumes = MediaType.APPLICATION_JSON_VALUE)
  public CssCompileResponse compileCss(@RequestBody CssCompileRequest request) {
    int fileCount = request.files() == null ? 0 : request.files().size();
    LOG.info(
        "HTTP CSS compile request received with {} files (input={})",
        fileCount,
        request.inputPath());
    return cssCompileService.compile(request);
  }

  /** Generates a Codename One project scaffold. */
  @PostMapping(value = "/cn1_scaffold_project", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ScaffoldResponse scaffold(@RequestBody ScaffoldRequest request) {
    LOG.info(
        "HTTP scaffold request received: package={} name={}", request.pkg(), request.name());
    return scaffoldService.scaffold(request);
  }

  /** Provides human-readable explanations for lint violations. */
  @PostMapping(value = "/cn1_explain_violation", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ExplainResponse explain(@RequestBody ExplainRequest request) {
    LOG.info("HTTP explain request received for rule {}", request.ruleId());
    return snippetService.explain(request.ruleId());
  }

  /** Looks up tutorial snippets for the requested topic. */
  @PostMapping(value = "/cn1_search_snippets", consumes = MediaType.APPLICATION_JSON_VALUE)
  public SnippetsResponse searchSnippets(@RequestBody SnippetsRequest request) {
    LOG.info("HTTP snippets search for topic {}", request.topic());
    return snippetService.get(request.topic());
  }

  /** Applies simple automatic fixes to known lint issues. */
  @PostMapping(value = "/cn1_auto_fix", consumes = MediaType.APPLICATION_JSON_VALUE)
  public AutoFixResponse autoFix(@RequestBody AutoFixRequest request) {
    String source = request.code();
    if (source == null) {
      // SpotBugs: HTTP clients may omit the code payload; use an empty string instead of risking
      // NPEs.
      source = "";
    }
    String patched =
        source.replace(
            "form.show();",
            "com.codename1.ui.Display.getInstance().callSerially(() -> { form.show(); });");
    LOG.info("HTTP auto-fix request received ({} chars)", source.length());
    String diff =
        String.join(
            "\n",
            "  @@",
            "  - form.show();",
            "  + com.codename1.ui.Display.getInstance().callSerially(() -> { form.show(); });",
            "  ");
    Patch patch = new Patch("Wrap show() in EDT", diff);
    return new AutoFixResponse(patched, List.of(patch));
  }

  /** Generates native interface stubs for Codename One projects. */
  @PostMapping(value = "/cn1_generate_native_stubs", consumes = MediaType.APPLICATION_JSON_VALUE)
  public NativeStubResponse generateNativeStubs(@RequestBody NativeStubRequest request) {
    int fileCount = request.files() == null ? 0 : request.files().size();
    LOG.info(
        "HTTP native stub generation request received for interface {} ({} source files)",
        request.interfaceName(),
        fileCount);
    return nativeStubService.generate(request);
  }
}
