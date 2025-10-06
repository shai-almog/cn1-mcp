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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tools")
public class ToolsController {
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
    public LintResponse lint(@RequestBody LintRequest req) { return lint.lint(req); }

    @PostMapping(value="/cn1_compile_check", consumes=MediaType.APPLICATION_JSON_VALUE)
    public CompileResponse compile(@RequestBody CompileRequest req) { return compile.compile(req); }

    @PostMapping(value="/cn1_scaffold_project", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ScaffoldResponse scaffold(@RequestBody ScaffoldRequest req) { return scaffold.scaffold(req); }

    @PostMapping(value="/cn1_explain_violation", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ExplainResponse explain(@RequestBody ExplainRequest req) { return snippets.explain(req.ruleId()); }

    @PostMapping(value="/cn1_search_snippets", consumes=MediaType.APPLICATION_JSON_VALUE)
    public SnippetsResponse snippets(@RequestBody SnippetsRequest req) { return snippets.get(req.topic()); }

    // Auto-fix can be naive: rewrap UI mutations; expand as needed
    @PostMapping(value="/cn1_auto_fix", consumes=MediaType.APPLICATION_JSON_VALUE)
    public AutoFixResponse autoFix(@RequestBody AutoFixRequest req) {
        String patched = req.code().replace("form.show();",
                "com.codename1.ui.Display.getInstance().callSerially(() -> { form.show(); });");
        var patch = new Patch("Wrap show() in EDT", """
      @@
      - form.show();
      + com.codename1.ui.Display.getInstance().callSerially(() -> { form.show(); });
      """);
        return new AutoFixResponse(patched, java.util.List.of(patch));
    }
}