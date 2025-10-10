package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.LintDiag;
import com.codename1.server.mcp.dto.LintRequest;
import com.codename1.server.mcp.dto.LintResponse;
import com.codename1.server.mcp.dto.QuickFix;
import com.codename1.server.mcp.dto.Range;
import com.codename1.server.mcp.rules.RulePack;
import com.codename1.server.mcp.tools.PatchUtil;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Executes linting heuristics against provided source code snippets. */
@Service
public class LintService {
  private static final Logger LOG = LoggerFactory.getLogger(LintService.class);
  private static final Range WHOLE_FILE =
      new Range(new Range.Pos(1, 1), new Range.Pos(1, 1));

  /**
   * Runs the lint rules on the provided request payload and aggregates diagnostics.
   */
  public LintResponse lint(LintRequest req) {
    String code = req.code();
    if (code == null) {
      // SpotBugs: requests may omit the code payload, so treat it as empty to avoid NPEs.
      code = "";
    }
    var diags = new ArrayList<LintDiag>();
    var fixes = new ArrayList<QuickFix>();

    LOG.info(
        "Running lint for language={} codeSize={}", req.language(), code.length());

    // 1) Forbidden imports
    var lines = code.split("\\R");
    for (int i = 0; i < lines.length; i++) {
      String ln = lines[i];
      for (Pattern p : RulePack.FORBIDDEN) {
        if (p.matcher(ln).find()) {
          diags.add(
              new LintDiag(
                  "CN1_FORBIDDEN_IMPORT",
                  "error",
                  "Forbidden import by strict rule.",
                  rng(i, ln)));
        }
      }
    }

    // 2) Forbidden FQNs
    for (Pattern p : RulePack.FORBIDDEN_FQNS) {
      var matcher = p.matcher(code);
      if (matcher.find()) {
        diags.add(
            new LintDiag(
                "CN1_FORBIDDEN_API",
                "error",
                "Forbidden API: " + matcher.group(),
                WHOLE_FILE));
      }
    }

    // 3) Absolute paths
    var absolute = RulePack.ABSOLUTE_PATH.matcher(code);
    if (absolute.find()) {
      diags.add(
          new LintDiag(
              "CN1_STORAGE_PATHS",
              "error",
              "Use Storage/FileSystemStorage/Preferences instead of absolute paths.",
              WHOLE_FILE));
    }

    // 4) EDT rule heuristic
    boolean hasUiMutation = RulePack.UI_MUTATION.matcher(code).find();
    boolean wrapped = RulePack.CALLS_SERIAL.matcher(code).find();
    if (hasUiMutation && !wrapped) {
      diags.add(
          new LintDiag(
              "CN1_EDT_RULE",
              "error",
              "UI mutations must occur on the EDT. Wrap in Display.getInstance().callSerially(...).",
              WHOLE_FILE));
      fixes.add(new QuickFix("Wrap UI code in callSerially(...)", PatchUtil.wrapEdtPatch()));
    }

    // 5) Raw threads & sleep (strict => error)
    if (RulePack.RAW_THREAD.matcher(code).find()) {
      diags.add(
          new LintDiag(
              "CN1_RAW_THREADS",
              "error",
              "Use CN.execute/NetworkManager instead of raw Thread.",
              WHOLE_FILE));
    }
    if (RulePack.SLEEP.matcher(code).find()) {
      diags.add(
          new LintDiag(
              "CN1_SLEEP_ON_EDT",
              "error",
              "Avoid Thread.sleep(); never block the EDT.",
              WHOLE_FILE));
    }

    // 6) Optional AST pass for imports that escaped regex
    try {
      CompilationUnit cu = StaticJavaParser.parse(code);
      cu.getImports()
          .forEach(
              imp -> {
                String q = imp.getNameAsString();
                if (q.startsWith("java.awt.")
                    || q.startsWith("javax.swing.")
                    || q.startsWith("javafx.")
                    || q.startsWith("java.nio.file.")
                    || q.startsWith("java.lang.reflect.")
                    || q.startsWith("java.sql.")) {
                  diags.add(
                      new LintDiag(
                          "CN1_FORBIDDEN_IMPORT",
                          "error",
                          "Forbidden import: " + q,
                          WHOLE_FILE));
                }
              });
    } catch (Exception ex) {
      // SpotBugs: parsing failures are expected on malformed sources; log and continue linting.
      LOG.debug("AST parse failed, continuing lint", ex);
    }

    LintResponse response = new LintResponse(diags.isEmpty(), diags, fixes);
    LOG.info(
        "Lint finished: ok={} diagnostics={} quickFixes={}",
        response.ok(),
        response.diagnostics().size(),
        response.quickFixes().size());
    return response;
  }

  private Range rng(int lineIndex, String lineText) {
    return new Range(
        new Range.Pos(lineIndex + 1, 1),
        new Range.Pos(lineIndex + 1, Math.max(1, lineText.length())));
  }
}
