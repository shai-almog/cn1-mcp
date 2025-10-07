package com.codename1.server.mcp.controller;

import com.codename1.server.mcp.dto.AutoFixRequest;
import com.codename1.server.mcp.dto.AutoFixResponse;
import com.codename1.server.mcp.dto.CompileRequest;
import com.codename1.server.mcp.dto.CompileResponse;
import com.codename1.server.mcp.dto.ExplainRequest;
import com.codename1.server.mcp.dto.ExplainResponse;
import com.codename1.server.mcp.dto.LintRequest;
import com.codename1.server.mcp.dto.LintResponse;
import com.codename1.server.mcp.dto.Patch;
import com.codename1.server.mcp.dto.ScaffoldRequest;
import com.codename1.server.mcp.dto.ScaffoldResponse;
import com.codename1.server.mcp.dto.SnippetsRequest;
import com.codename1.server.mcp.dto.SnippetsResponse;
import com.codename1.server.mcp.service.ExternalCompileService;
import com.codename1.server.mcp.service.LintService;
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
    private final ScaffoldService scaffold;
    private final SnippetService snippets;

    public ToolsController(LintService l, ExternalCompileService c, ScaffoldService s, SnippetService sn) {
        this.lint = l;
        this.compile = c;
        this.scaffold = s;
        this.snippets = sn;
    }

    @PostMapping(value="/cn1_lint_code", consumes=MediaType.APPLICATION_JSON_VALUE)
    public LintResponse lint(@RequestBody LintRequest req) {
        LOG.info("HTTP lint request received: language={}, codeLength={}", req.language(), req.code() != null ? req.code().length() : 0);
        LintResponse res = lint.lint(req);
        LOG.debug("Lint response ok={}, diagnostics={}, quickFixes={}", res.ok(), res.diagnostics().size(), res.quickFixes().size());
        return res;
    }

    @PostMapping(value="/cn1_compile_check", consumes=MediaType.APPLICATION_JSON_VALUE)
    public CompileResponse compile(@RequestBody CompileRequest req) {
        LOG.info("HTTP compile request received with {} file(s)", req.files() != null ? req.files().size() : 0);
        CompileResponse res = compile.compile(req);
        LOG.debug("Compile response ok={}, outputLength={}", res.ok(), res.javacOutput() != null ? res.javacOutput().length() : 0);
        return res;
    }

    @PostMapping(value="/cn1_scaffold_project", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ScaffoldResponse scaffold(@RequestBody ScaffoldRequest req) {
        LOG.info("HTTP scaffold request received: pkg={}, name={}", req.pkg(), req.name());
        ScaffoldResponse res = scaffold.scaffold(req);
        LOG.debug("Scaffold response contains {} file(s)", res.files().size());
        return res;
    }

    @PostMapping(value="/cn1_explain_violation", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ExplainResponse explain(@RequestBody ExplainRequest req) {
        LOG.info("HTTP explain request received for rule {}", req.ruleId());
        return snippets.explain(req.ruleId());
    }

    @PostMapping(value="/cn1_search_snippets", consumes=MediaType.APPLICATION_JSON_VALUE)
    public SnippetsResponse snippets(@RequestBody SnippetsRequest req) {
        LOG.info("HTTP snippets request received for topic {}", req.topic());
        SnippetsResponse response = snippets.get(req.topic());
        LOG.debug("Returned {} snippet(s) for topic {}", response.snippets().size(), req.topic());
        return response;
    }

    // Auto-fix can be naive: rewrap UI mutations; expand as needed
    @PostMapping(value="/cn1_auto_fix", consumes=MediaType.APPLICATION_JSON_VALUE)
    public AutoFixResponse autoFix(@RequestBody AutoFixRequest req) {
        LOG.info("HTTP auto-fix request received");
        String patched = req.code().replace("form.show();",
                "com.codename1.ui.Display.getInstance().callSerially(() -> { form.show(); });");
        var patch = new Patch("Wrap show() in EDT", """
      @@
      - form.show();
      + com.codename1.ui.Display.getInstance().callSerially(() -> { form.show(); });
      """);
        AutoFixResponse res = new AutoFixResponse(patched, java.util.List.of(patch));
        LOG.debug("Auto-fix generated {} patch(es)", res.patches().size());
        return res;
    }
}