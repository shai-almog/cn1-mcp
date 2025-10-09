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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tools")
public class ToolsController {
    private static final Logger LOG = LoggerFactory.getLogger(ToolsController.class);
    private final LintService lint;
    private final ExternalCompileService compile;
    private final CssCompileService cssCompile;
    private final ScaffoldService scaffold;
    private final SnippetService snippets;
    private final NativeStubService nativeStubs;

    public ToolsController(LintService l, ExternalCompileService c, CssCompileService css, ScaffoldService s, SnippetService sn, NativeStubService ns) {
        this.lint = l;
        this.compile = c;
        this.cssCompile = css;
        this.scaffold = s;
        this.snippets = sn;
        this.nativeStubs = ns;
    }

    @PostMapping(value="/cn1_lint_code", consumes=MediaType.APPLICATION_JSON_VALUE)
    public LintResponse lint(@RequestBody LintRequest req) {
        LOG.info("HTTP lint request received: language={} rulesetSize={}", req.language(), req.ruleset() == null ? 0 : req.ruleset().size());
        return lint.lint(req);
    }

    @PostMapping(value="/cn1_compile_check", consumes=MediaType.APPLICATION_JSON_VALUE)
    public CompileResponse compile(@RequestBody CompileRequest req) {
        LOG.info("HTTP compile request received with {} files", req.files().size());
        return compile.compile(req);
    }

    @PostMapping(value="/cn1_compile_css", consumes=MediaType.APPLICATION_JSON_VALUE)
    public CssCompileResponse compileCss(@RequestBody CssCompileRequest req) {
        int fileCount = req.files() != null ? req.files().size() : 0;
        LOG.info("HTTP CSS compile request received with {} files (input={})", fileCount, req.inputPath());
        return cssCompile.compile(req);
    }

    @PostMapping(value="/cn1_scaffold_project", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ScaffoldResponse scaffold(@RequestBody ScaffoldRequest req) {
        LOG.info("HTTP scaffold request received: package={} name={}", req.pkg(), req.name());
        return scaffold.scaffold(req);
    }

    @PostMapping(value="/cn1_explain_violation", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ExplainResponse explain(@RequestBody ExplainRequest req) {
        LOG.info("HTTP explain request received for rule {}", req.ruleId());
        return snippets.explain(req.ruleId());
    }

    @PostMapping(value="/cn1_search_snippets", consumes=MediaType.APPLICATION_JSON_VALUE)
    public SnippetsResponse snippets(@RequestBody SnippetsRequest req) {
        LOG.info("HTTP snippets search for topic {}", req.topic());
        return snippets.get(req.topic());
    }

    // Auto-fix can be naive: rewrap UI mutations; expand as needed
    @PostMapping(value="/cn1_auto_fix", consumes=MediaType.APPLICATION_JSON_VALUE)
    public AutoFixResponse autoFix(@RequestBody AutoFixRequest req) {
        String patched = req.code().replace("form.show();",
                "com.codename1.ui.Display.getInstance().callSerially(() -> { form.show(); });");
        LOG.info("HTTP auto-fix request received ({} chars)", req.code() != null ? req.code().length() : 0);
        var patch = new Patch("Wrap show() in EDT", """
      @@
      - form.show();
      + com.codename1.ui.Display.getInstance().callSerially(() -> { form.show(); });
      """);
        return new AutoFixResponse(patched, java.util.List.of(patch));
    }

    @PostMapping(value="/cn1_generate_native_stubs", consumes=MediaType.APPLICATION_JSON_VALUE)
    public NativeStubResponse generateNativeStubs(@RequestBody NativeStubRequest req) {
        int fileCount = req.files() != null ? req.files().size() : 0;
        LOG.info("HTTP native stub generation request received for interface {} ({} source files)", req.interfaceName(), fileCount);
        return nativeStubs.generate(req);
    }
}