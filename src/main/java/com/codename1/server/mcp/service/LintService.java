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
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.regex.Pattern;

@Service
public class LintService {
    public LintResponse lint(LintRequest req) {
        var code = req.code();
        var diags = new ArrayList<LintDiag>();
        var fixes = new ArrayList<QuickFix>();

        // 1) Forbidden imports
        var lines = code.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String ln = lines[i];
            for (Pattern p : RulePack.FORBIDDEN) {
                if (p.matcher(ln).find()) {
                    diags.add(new LintDiag("CN1_FORBIDDEN_IMPORT","error",
                            "Forbidden import by strict rule.", rng(i, ln)));
                }
            }
        }

        // 2) Forbidden FQNs
        for (Pattern p : RulePack.FORBIDDEN_FQNS) {
            var m = p.matcher(code);
            if (m.find()) {
                diags.add(new LintDiag("CN1_FORBIDDEN_API","error",
                        "Forbidden API: " + m.group(), new Range(new Range.Pos(1,1), new Range.Pos(1,1))));
            }
        }

        // 3) Absolute paths
        var m = RulePack.ABSOLUTE_PATH.matcher(code);
        if (m.find()) {
            diags.add(new LintDiag("CN1_STORAGE_PATHS","error",
                    "Use Storage/FileSystemStorage/Preferences instead of absolute paths.",
                    new Range(new Range.Pos(1,1), new Range.Pos(1,1))));
        }

        // 4) EDT rule heuristic
        boolean hasUiMutation = RulePack.UI_MUTATION.matcher(code).find();
        boolean wrapped = RulePack.CALLS_SERIAL.matcher(code).find();
        if (hasUiMutation && !wrapped) {
            diags.add(new LintDiag("CN1_EDT_RULE","error",
                    "UI mutations must occur on the EDT. Wrap in Display.getInstance().callSerially(...).",
                    new Range(new Range.Pos(1,1), new Range.Pos(1,1))));
            fixes.add(new QuickFix("Wrap UI code in callSerially(...)",
                    PatchUtil.wrapEdtPatch()));
        }

        // 5) Raw threads & sleep (strict => error)
        if (RulePack.RAW_THREAD.matcher(code).find()) {
            diags.add(new LintDiag("CN1_RAW_THREADS","error",
                    "Use CN.execute/NetworkManager instead of raw Thread.", new Range(new Range.Pos(1,1), new Range.Pos(1,1))));
        }
        if (RulePack.SLEEP.matcher(code).find()) {
            diags.add(new LintDiag("CN1_SLEEP_ON_EDT","error",
                    "Avoid Thread.sleep(); never block the EDT.", new Range(new Range.Pos(1,1), new Range.Pos(1,1))));
        }

        // 6) Optional AST pass for imports that escaped regex
        try {
            CompilationUnit cu = StaticJavaParser.parse(code);
            cu.getImports().forEach(imp -> {
                String q = imp.getNameAsString();
                if (q.startsWith("java.awt.") || q.startsWith("javax.swing.") ||
                        q.startsWith("javafx.") || q.startsWith("java.nio.file.") ||
                        q.startsWith("java.lang.reflect.") || q.startsWith("java.sql.")) {
                    diags.add(new LintDiag("CN1_FORBIDDEN_IMPORT","error",
                            "Forbidden import: " + q, new Range(new Range.Pos(1,1), new Range.Pos(1,1))));
                }
            });
        } catch (Exception ignored) { /* keep lint robust */ }

        return new LintResponse(diags.isEmpty(), diags, fixes);
    }

    private Range rng(int i, String ln) {
        return new Range(new Range.Pos(i+1, 1), new Range.Pos(i+1, Math.max(1, ln.length())));
    }
}