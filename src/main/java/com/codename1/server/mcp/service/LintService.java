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

/** Evaluates lint rules against submitted code and produces diagnostics and quick fixes. */
@Service
public class LintService {

  private static final Logger LOG = LoggerFactory.getLogger(LintService.class);

  /**
   * Runs the configured lint rules on the provided request payload.
   *
   * @param req lint request containing the code to analyze
   * @return the lint response including diagnostics and quick fixes
   */
  public LintResponse lint(LintRequest req) {
    String code = req.code();
    if (code == null) {
      // SpotBugs: requests may omit the code payload, so treat it as empty to avoid NPEs.
      code = "";
    }
    var diags = new ArrayList<LintDiag>();
    var fixes = new ArrayList<QuickFix>();

    LOG.info("Running lint for language={} codeSize={}", req.language(), code.length());

    // 1) Forbidden imports
    var lines = code.split("\\R");
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      for (Pattern pattern : RulePack.FORBIDDEN) {
        if (pattern.matcher(line).find()) {
          diags.add(
              new LintDiag(
                  "CN1_FORBIDDEN_IMPORT",
                  "error",
                  "Forbidden import by strict rule.",
                  rng(i, line)));
        }
      }
    }

    Range wholeFile = new Range(new Range.Pos(1, 1), new Range.Pos(1, 1));

    // 2) Forbidden FQNs
    for (Pattern pattern : RulePack.FORBIDDEN_FQNS) {
      var matcher = pattern.matcher(code);
      if (matcher.find()) {
        diags.add(
            new LintDiag(
                "CN1_FORBIDDEN_API", "error", "Forbidden API: " + matcher.group(), wholeFile));
      }
    }

    // 3) Absolute paths
    var absoluteMatcher = RulePack.ABSOLUTE_PATH.matcher(code);
    if (absoluteMatcher.find()) {
      diags.add(
          new LintDiag(
              "CN1_STORAGE_PATHS",
              "error",
              "Use Storage/FileSystemStorage/Preferences instead of absolute paths.",
              wholeFile));
    }

    // 4) EDT rule heuristic
    boolean hasUiMutation = RulePack.UI_MUTATION.matcher(code).find();
    boolean wrapped = RulePack.CALLS_SERIAL.matcher(code).find();
    if (hasUiMutation && !wrapped) {
      diags.add(
          new LintDiag(
              "CN1_EDT_RULE",
              "error",
              "UI mutations must occur on the EDT. Wrap in"
                  + " Display.getInstance().callSerially(...).",
              wholeFile));
      fixes.add(new QuickFix("Wrap UI code in callSerially(...)", PatchUtil.wrapEdtPatch()));
    }

    // 5) Raw threads & sleep (strict => error)
    if (RulePack.RAW_THREAD.matcher(code).find()) {
      diags.add(
          new LintDiag(
              "CN1_RAW_THREADS",
              "error",
              "Use CN.execute/NetworkManager instead of raw Thread.",
              wholeFile));
    }
    if (RulePack.SLEEP.matcher(code).find()) {
      diags.add(
          new LintDiag(
              "CN1_SLEEP_ON_EDT",
              "error",
              "Avoid Thread.sleep(); never block the EDT.",
              wholeFile));
    }

    // 6) Optional AST pass for imports that escaped regex
    try {
      CompilationUnit cu = StaticJavaParser.parse(code);
      cu.getImports()
          .forEach(
              imp -> {
                String qualified = imp.getNameAsString();
                if (qualified.startsWith("java.awt.")
                    || qualified.startsWith("javax.swing.")
                    || qualified.startsWith("javafx.")
                    || qualified.startsWith("java.nio.file.")
                    || qualified.startsWith("java.lang.reflect.")
                    || qualified.startsWith("java.sql.")) {
                  diags.add(
                      new LintDiag(
                          "CN1_FORBIDDEN_IMPORT",
                          "error",
                          "Forbidden import: " + qualified,
                          wholeFile));
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

  private Range rng(int index, String line) {
    return new Range(
        new Range.Pos(index + 1, 1), new Range.Pos(index + 1, Math.max(1, line.length())));
  }
}
